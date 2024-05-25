package com.zenith.database;

import io.netty.resolver.DefaultAddressResolverGroup;
import lombok.Getter;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.DATABASE_LOG;
import static java.util.Objects.isNull;

@Getter
public class RedisClient {

    private RedissonClient redissonClient;

    public RedisClient() {
        redissonClient = buildRedisClient();
    }

    public RLock getLock(final String lockKey) {
        synchronized (this) {
            if (isShutDown()) {
                redissonClient = buildRedisClient();
            }
            return redissonClient.getLock(lockKey);
        }
    }

    public void unlock(final RLock lock) {
        synchronized (this) {
            try {
                lock.unlock();
            } catch (final Throwable e) {
                redissonClient.shutdown();
                DATABASE_LOG.warn("Unlock threw exception", e);
            }
        }
    }

    public boolean isShutDown() {
        return isNull(redissonClient) || redissonClient.isShuttingDown() || redissonClient.isShutdown();
    }

    public static RedissonClient buildRedisClient() {
        Config config = new Config();
        config.setNettyThreads(1)
            .setAddressResolverGroupFactory((channelType, socketChannelType, nameServerProvider) -> DefaultAddressResolverGroup.INSTANCE)
            .setThreads(1)
            .useSingleServer()
            .setAddress(CONFIG.database.lock.redisAddress)
            .setUsername(CONFIG.database.lock.redisUsername)
            .setPassword(CONFIG.database.lock.redisPassword)
            .setConnectionPoolSize(1)
            .setConnectionMinimumIdleSize(1);
        config.setLockWatchdogTimeout(15000);
        return Redisson.create(config);
    }
}
