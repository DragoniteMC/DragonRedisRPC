package org.dragonite.mc.redisrpc;

import com.ericlam.mc.eld.services.ScheduleService;

import java.util.concurrent.CompletableFuture;

public interface RedisRPCDemoService {

    ScheduleService.BukkitPromise<String> greeting(String s);

    ScheduleService.BukkitPromise<String> getFromRedis(String key);

    ScheduleService.BukkitPromise<String> setToRedis(String key, String value);

}
