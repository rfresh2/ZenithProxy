package com.zenith.database;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import static com.zenith.util.Constants.CONFIG;
import static java.util.Objects.isNull;

public class RedisClient {

    private RedissonClient redissonClient;

    public RedisClient() {

    }

    public RLock getLock(final String lockKey) {
        synchronized (this) {
            if (isNull(redissonClient)) {
                Config config = new Config();
                config.useSingleServer()
                        .setAddress(CONFIG.database.lock.redisAddress)
                        .setUsername(CONFIG.database.lock.redisUsername)
                        .setPassword(CONFIG.database.lock.redisPassword)
                        .setConnectionPoolSize(1)
                        .setConnectionMinimumIdleSize(1);
                config.setLockWatchdogTimeout(5000);
                redissonClient = Redisson.create(config);
            }
            return redissonClient.getLock(lockKey);
        }
    }

    public void unlock(final RLock lock) {
        synchronized (this) {
            try {
                lock.unlock();
            } catch (final Throwable e) {
                // todo: this always throws for some reason on stop()
                //  but the watchdog will release the lock eventually anyway
                // DATABASE_LOG.warn("Unlock threw exception", e);
            }
            redissonClient.shutdown();
            redissonClient = null;
        }
    }
}
