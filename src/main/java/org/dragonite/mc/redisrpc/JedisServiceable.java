package org.dragonite.mc.redisrpc;

import com.dragonite.mc.dnmc.core.main.DragoniteMC;
import com.ericlam.mc.eld.misc.DebugLogger;
import com.ericlam.mc.eld.services.LoggingService;
import com.ericlam.mc.eld.services.ScheduleService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eldependenci.rpc.context.RPCPayload;
import org.eldependenci.rpc.context.RPCResponse;
import org.eldependenci.rpc.context.RPCResult;
import org.eldependenci.rpc.protocol.RPCServiceable;
import org.eldependenci.rpc.protocol.ServiceHandler;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class JedisServiceable extends BinaryJedisPubSub implements RPCServiceable {

    private ServiceHandler handler;

    @Inject
    @Named("eld-json")
    private ObjectMapper mapper;


    private final DebugLogger logger;

    @Inject
    public JedisServiceable(LoggingService logging){
        this.logger = logging.getLogger(JedisServiceable.class);
    }

    @Override
    public void StartService(ServiceHandler handler) {
        this.handler = handler;
        try(Jedis jedis = DragoniteMC.getAPI().getRedisDataSource().getJedis()){
            jedis.subscribe(this, DragonRedisRPC.PRODUCER_CHANNEL.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public void StopService() {
        this.unsubscribe();
    }

    @Override
    public void onMessage(byte[] channel, byte[] message) {
        var channelName = new String(channel);
        if (!channelName.equals(DragonRedisRPC.PRODUCER_CHANNEL)) {
            logger.debug("unknown channel received: {0}", channelName);
            return;
        }

        try {

            RPCPayload rpcPayload = mapper.readValue(message, RPCPayload.class);

            var methodName = rpcPayload.method();
            var serviceName = rpcPayload.service();

            logger.debug("looking for method {0} in service {1}", methodName, serviceName);

            var future = handler.handlePayload(rpcPayload, false);

            future.thenAcceptAsync(res -> {

                logger.debug("sending result to redis: {0}", res);

                try(Jedis jedis = DragoniteMC.getAPI().getRedisDataSource().getJedis()){
                    var response = new RPCResponse<>(rpcPayload.id(), res instanceof RPCResult, res);
                    jedis.publish(DragonRedisRPC.CONSUMER_CHANNEL.getBytes(StandardCharsets.UTF_8), mapper.writeValueAsBytes(response));
                } catch (JsonProcessingException e) {
                    throw new CompletionException(e);
                }

            }).whenComplete((v, ex) -> {
                if (ex != null){
                    logger.warn("error occurred while handling RPC payload: {0}", ex.getMessage());
                    logger.warn(ex);
                }
            });

        }catch (Exception e){

            logger.warn("error occurred while handling RPC payload: {0}", e.getMessage());
            logger.debug(e);

            try(Jedis jedis = DragoniteMC.getAPI().getRedisDataSource().getJedis()){
                var response = new RPCResponse<>(-1, false, handler.toRPCError(e, false));
                jedis.publish(DragonRedisRPC.CONSUMER_CHANNEL.getBytes(StandardCharsets.UTF_8), mapper.writeValueAsBytes(response));
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(e);
            }
        }
    }


    @Override
    public void onSubscribe(byte[] channel, int subscribedChannels) {
        logger.infoF("成功啟動 %s 頻道的 PubSub 訂閱監聽。", new String(channel));
    }

    @Override
    public void onUnsubscribe(byte[] channel, int subscribedChannels) {
        logger.infoF("成功取消 %s 頻道的 PubSub 訂閱監聽。", new String(channel));
    }
}
