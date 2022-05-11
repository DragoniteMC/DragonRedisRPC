package org.dragonite.mc.redisrpc;

import com.dragonite.mc.dnmc.core.main.DragoniteMC;
import com.ericlam.mc.eld.misc.DebugLogger;
import com.ericlam.mc.eld.services.LoggingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eldependenci.rpc.context.*;
import org.eldependenci.rpc.protocol.RPCRequester;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class JedisRequester extends BinaryJedisPubSub implements RPCRequester {

    private final DebugLogger logger;


    @Inject
    @Named("eld-json")
    private ObjectMapper mapper;

    private final Map<Long, Consumer<Object>> callbackMap = new ConcurrentHashMap<>();

    @Inject
    public JedisRequester(LoggingService loggingService) {
        this.logger = loggingService.getLogger(JedisRequester.class);
    }

    @Override
    public CompletableFuture<Void> initialize(RPCInfo client) {
        return CompletableFuture.runAsync(() -> {
            try (Jedis jedis = DragoniteMC.getAPI().getRedisDataSource().getJedis()) {
                jedis.subscribe(this, DragonRedisRPC.PRODUCER_CHANNEL.getBytes(StandardCharsets.UTF_8));
            }
        });
    }

    @Override
    public CompletableFuture<Object> offerRequest(RPCPayload payload) {
        var future = new CompletableFuture<Object>();
        var id = payload.id();
        callbackMap.put(id, o -> {
            if (o instanceof RPCError err) {
                future.completeExceptionally(new Exception(err.message()));
            } else {
                future.complete(o);
            }
        });
        try (Jedis jedis = DragoniteMC.getAPI().getRedisDataSource().getJedis()) {
            var res = mapper.writeValueAsBytes(payload);
            jedis.publish(DragonRedisRPC.CONSUMER_CHANNEL.getBytes(StandardCharsets.UTF_8), res);
        } catch (IOException e) {
            future.completeExceptionally(e);
        }
        return future;
    }


    @Override
    public void onMessage(byte[] channel, byte[] message) {
        var channelName = new String(channel);
        if (!channelName.equals(DragonRedisRPC.PRODUCER_CHANNEL)) {
            logger.debug("unknown channel received: {0}", channelName);
            return;
        }
        try {

            var res = mapper.readValue(message, RPCResponse.class);

            if (res.success()){
                var result = mapper.convertValue(res.result(), RPCResult.class);
                var callback = callbackMap.remove(res.id());
                if (callback != null) {
                    callback.accept(result.result());
                }
            } else {
                var err = mapper.convertValue(res.result(), RPCError.class);
                var callback = callbackMap.remove(res.id());
                if (callback != null) {
                    callback.accept(err);
                }
            }

        } catch (IOException e) {
            logger.warn("failed to parse response: {0}", e.getMessage());
            logger.debug(e);
        }
    }
}
