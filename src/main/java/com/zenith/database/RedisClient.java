package com.zenith.database;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import static com.zenith.util.Constants.CONFIG;
import static com.zenith.util.Constants.DATABASE_LOG;
import static java.util.Objects.isNull;

public class RedisClient {

    private RedissonClient redissonClient;

    public RedisClient() {
        redissonClient = getRedissonClient();
    }

    public RLock getLock(final String lockKey) {
        synchronized (this) {
            if (isShutDown()) {
                redissonClient = getRedissonClient();
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

    public RedissonClient getRedissonClient() {
        Config config = new Config();
        config.setNettyThreads(1)
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
