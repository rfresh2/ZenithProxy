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
        if (isNull(redissonClient)) {
            Config config = new Config();
            config.useSingleServer()
                    .setAddress(CONFIG.database.lock.redisAddress)
                    .setUsername(CONFIG.database.lock.redisUsername)
                    .setPassword(CONFIG.database.lock.redisPassword);
            redissonClient = Redisson.create(config);
        }
        return redissonClient.getLock(lockKey);
    }
}
