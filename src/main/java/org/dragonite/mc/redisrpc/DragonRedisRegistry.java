package org.dragonite.mc.redisrpc;

import com.ericlam.mc.eld.registrations.CommandRegistry;
import com.ericlam.mc.eld.registrations.ComponentsRegistry;
import com.ericlam.mc.eld.registrations.ListenerRegistry;

public class DragonRedisRegistry implements ComponentsRegistry {
    @Override
    public void registerCommand(CommandRegistry commandRegistry) {
        commandRegistry.command(RedisRPCCommand.class);
    }

    @Override
    public void registerListeners(ListenerRegistry listenerRegistry) {

    }
}
