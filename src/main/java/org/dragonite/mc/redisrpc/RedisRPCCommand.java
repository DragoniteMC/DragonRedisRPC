package org.dragonite.mc.redisrpc;

import com.ericlam.mc.eld.annotations.Commander;
import com.ericlam.mc.eld.components.CommandNode;
import com.ericlam.mc.eld.services.ScheduleService;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;

@Commander(
        name = "redisrpc",
        description = "RedisRPC Test Command",
        permission = "dragonite.redisrpc.test"
)
public class RedisRPCCommand implements CommandNode {

    @Inject
    private RedisRPCDemoService demoService;

    @Inject
    private DragonRedisRPC plugin;

    @Inject
    private ScheduleService scheduler;

    @Override
    public void execute(CommandSender commandSender) {

       scheduler.runAsync(plugin, () -> {
          var r = demoService.greeting(commandSender.getName());
          commandSender.sendMessage("greeting 返回: " + r);
          demoService.setToRedis("hello", "world");
          commandSender.sendMessage("setToRedis 成功");
          var r2 = demoService.getFromRedis("hello");
          commandSender.sendMessage("getFromRedis 返回: " + r2);
       }).join();
    }
}
