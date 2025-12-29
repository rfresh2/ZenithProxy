package com.zenith.database;

import com.zenith.event.db.RedisRestartEvent;
import io.netty.resolver.DefaultAddressResolverGroup;
import lombok.Getter;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;

import java.time.Instant;

import static com.zenith.Globals.*;
import static java.util.Objects.isNull;

@Getter
public class RedisClient {

    private RedissonClient redissonClient = buildRedisClient();

    public RLock getLock(final String lockKey) {
        synchronized (this) {
            return redissonClient.getLock(lockKey);
        }
    }

    public void unlock(final RLock lock) {
        synchronized (this) {
            try {
                lock.unlock();
            } catch (final Throwable e) {
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
            if (redissonClient != null && !isShutDown()) {
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
            .setUsername(CONFIG.database.lock.redisUsername)
            .setPassword(CONFIG.database.lock.redisPassword)
            .useSingleServer()
            .setAddress(CONFIG.database.lock.redisAddress)
            .setConnectionPoolSize(1)
            .setConnectionMinimumIdleSize(1);
        config.setLockWatchdogTimeout(15000);
        config.setCodec(StringCodec.INSTANCE);
        return Redisson.create(config);
    }
}
