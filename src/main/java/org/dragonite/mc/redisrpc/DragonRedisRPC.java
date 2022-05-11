package org.dragonite.mc.redisrpc;

import com.dragonite.mc.dnmc.core.main.DragoniteMC;
import com.ericlam.mc.eld.ELDBukkitPlugin;
import com.ericlam.mc.eld.ManagerProvider;
import com.ericlam.mc.eld.ServiceCollection;
import com.ericlam.mc.eld.annotations.ELDPlugin;
import org.eldependenci.rpc.RPCInstallation;


@ELDPlugin(
        lifeCycle = DragonRedisLifeCycle.class,
        registry = DragonRedisRegistry.class
)
public final class DragonRedisRPC extends ELDBukkitPlugin {

    private static final String REDIS_PROTOCOL = "REDIS";

    static final String PRODUCER_CHANNEL = "Dragonite.RPC.Producer";
    static final String CONSUMER_CHANNEL = "Dragonite.RPC.Consumer";

    @Override
    protected void manageProvider(ManagerProvider managerProvider) {

    }

    @Override
    protected void bindServices(ServiceCollection serviceCollection) {

        try {
            DragoniteMC.getAPI().getRedisDataSource();
        }catch (IllegalStateException e){
            getLogger().warning("DNMC 的 Redis 數據源加載失敗: "+e.getMessage());
            getLogger().warning("取消註冊 Redis RPC 服務。");
            return;
        }

        RPCInstallation installation = serviceCollection.getInstallation(RPCInstallation.class);
        installation.registerProtocol(REDIS_PROTOCOL, JedisServiceable.class, JedisRequester.class);
        installation.remotes(RedisRPCDemoService.class);
        installation.serves(RedisRPCTestService.class);
    }
}
