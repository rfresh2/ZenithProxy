package com.zenith.database;

import org.jooq.Query;
import org.redisson.api.RBoundedBlockingQueue;

import java.time.Instant;

import static com.zenith.Shared.DATABASE_LOG;
import static com.zenith.Shared.OBJECT_MAPPER;
import static java.util.Objects.isNull;

public abstract class LiveDatabase extends LockingDatabase {

    protected RBoundedBlockingQueue<String> queue = null;

    public LiveDatabase(final QueryExecutor queryExecutor, final RedisClient redisClient) {
        super(queryExecutor, redisClient);
    }

    public void insert(final Instant instant, final Object pojo, final Query query) {
        insert(instant, () -> liveQueueRunnable(pojo), query);
    }

    private String getQueueKey() {
        return getLockKey() + "Queue";
    }

    // todo: should we not extend locking database queue system so we can move live impl out of locking database?
    // todo: refactor locking database class into hierarchy

    void liveQueueRunnable(Object pojo) {
        if (isNull(queue)) {
            queue = redisClient.getRedissonClient().getBoundedBlockingQueue(getQueueKey());
            queue.trySetCapacity(50);
        }
        try {
            String json = OBJECT_MAPPER.writeValueAsString(pojo);
            queue.offerAsync(json).thenAcceptAsync((success) -> {
                if (!success) {
                    DATABASE_LOG.warn("{} reached capacity, flushing queue", getQueueKey());
                    queue.clear();
                }
            });
        } catch (final Exception e) {
            DATABASE_LOG.error("Failed to offer record to: {}", getQueueKey(), e);
        }
    }

}
