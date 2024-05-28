package com.zenith.database;

import lombok.Getter;
import org.jdbi.v3.core.HandleConsumer;
import org.redisson.api.RBoundedBlockingQueue;

import java.time.Instant;

import static com.zenith.Shared.DATABASE_LOG;
import static com.zenith.Shared.OBJECT_MAPPER;

public abstract class LiveDatabase extends LockingDatabase {

    @Getter(lazy = true)
    private final RBoundedBlockingQueue<String> queue = buildQueue();

    public LiveDatabase(final QueryExecutor queryExecutor, final RedisClient redisClient) {
        super(queryExecutor, redisClient);
    }

    private RBoundedBlockingQueue<String> buildQueue() {
        final RBoundedBlockingQueue<String> q = redisClient.getRedissonClient().getBoundedBlockingQueue(getQueueKey());
        q.trySetCapacity(500);
        return q;
    }

    public void insert(final Instant instant, final Object pojo, final HandleConsumer query) {
        insert(instant, () -> liveQueueRunnable(pojo), query);
    }

    private String getQueueKey() {
        return getLockKey() + "Queue";
    }

    // todo: should we not extend locking database queue system so we can move live impl out of locking database?
    // todo: refactor locking database class into hierarchy

    void liveQueueRunnable(Object pojo) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(pojo);
            getQueue().offerAsync(json).thenAcceptAsync((success) -> {
                if (!success) {
                    DATABASE_LOG.warn("{} reached capacity, flushing queue", getQueueKey());
                    getQueue().clear();
                }
            });
        } catch (final Exception e) {
            DATABASE_LOG.error("Failed to offer record to: {}", getQueueKey(), e);
        }
    }

}
