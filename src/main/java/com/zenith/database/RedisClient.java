package com.zenith.database;

import com.zenith.event.proxy.RedisRestartEvent;
import io.netty.resolver.DefaultAddressResolverGroup;
import lombok.Getter;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.time.Instant;

import static com.zenith.Shared.*;
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

    private Instant lastRestart = Instant.EPOCH;

    public void restart() {
        synchronized (this) {
            if (Instant.now().isBefore(lastRestart.plusSeconds(300))) {
                // hacky prevention of multiple locking db instances all hitting this
                DATABASE_LOG.info("Ignoring redis restart request, last restart was less than 30 seconds ago");
                return;
            }
            lastRestart = Instant.now();
            if (redissonClient != null) {
                try {
                    redissonClient.shutdown();
                } catch (final Throwable e) {
                    DATABASE_LOG.warn("Failed to shutdown redisson client", e);
                }
            }
            redissonClient = buildRedisClient();
            EVENT_BUS.postAsync(RedisRestartEvent.INSTANCE);
        }
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
