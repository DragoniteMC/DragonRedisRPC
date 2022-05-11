package org.dragonite.mc.redisrpc;

import com.dragonite.mc.dnmc.core.main.DragoniteMC;
import org.eldependenci.rpc.annotation.DoAsync;
import redis.clients.jedis.Jedis;
public class RedisRPCTestService {

    public String greeting(String s){
        return "Hello " + s;
    }

    @DoAsync
    public String getFromRedis(String key){
        try(Jedis jedis = DragoniteMC.getAPI().getRedisDataSource().getJedis()){
            return jedis.get(key);
        }
    }

    @DoAsync
    public void putToRedis(String key, String value){
        try(Jedis jedis = DragoniteMC.getAPI().getRedisDataSource().getJedis()){
            jedis.set(key, value);
        }
    }

}
