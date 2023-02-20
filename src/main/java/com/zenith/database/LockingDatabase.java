package com.zenith.database;

import com.zenith.Proxy;
import com.zenith.util.Wait;
import lombok.Data;
import org.jooq.Query;
import org.redisson.api.RLock;

import java.time.Duration;
import java.time.Instant;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.zenith.util.Constants.DATABASE_LOG;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Base class for databases that require a lock to be acquired before writing
 */
public abstract class LockingDatabase extends Database {
    private static final int maxQueueLen = 500;
    protected final Queue<InsertInstance> insertQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean lockAcquired = new AtomicBoolean(false);
    private final RedisClient redisClient;
    private RLock rLock;
    private ScheduledExecutorService lockExecutorService;
    private ScheduledExecutorService queryExecutorPool;

    public LockingDatabase(final QueryExecutor queryExecutor, final RedisClient redisClient) {
        super(queryExecutor);
        this.redisClient = redisClient;
    }

    public abstract String getLockKey();

    /**
     * Query the database, get latest entry
     * Then drop all records later than that in our queue
     */
    public abstract Instant getLastEntryTime();

    /**
     * Sync the current insert queue with what is in the remote database
     * intended as a deduping mechanism
     */
    public void syncQueue() {
        try {
            final long lastRecordSeenTimeEpochMs = getLastEntryTime().toEpochMilli();
            synchronized (this.insertQueue) {
                while (nonNull(this.insertQueue.peek()) && this.insertQueue.peek().getInstant().toEpochMilli()
                        <= lastRecordSeenTimeEpochMs + 5000 // buffer for latency or time shift
                ) {
                    this.insertQueue.poll();
                }
            }
        } catch (final Exception e) {
            DATABASE_LOG.warn("Error syncing {} database queue", getLockKey(), e);
        }
    }

    @Override
    public void start() {
        super.start();
        this.lockAcquired.set(false);
        synchronized (this) {
            if (isNull(lockExecutorService)) {
                lockExecutorService = Executors.newSingleThreadScheduledExecutor();
            }
        }
        lockExecutorService.scheduleAtFixedRate(this::tryLockProcess, 5000L, 1000L, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        super.stop();
        synchronized (this) {
            if (nonNull(lockExecutorService)) {
                try {
                    lockExecutorService.submit(() -> {
                        releaseLock();
                        onLockReleased();
                    }, true).get(5, TimeUnit.SECONDS);
                } catch (final Exception e) {
                    DATABASE_LOG.warn("Failed releasing lock", e);
                }
                lockExecutorService.shutdownNow();
                lockExecutorService = null;
            }
            lockAcquired.set(false);
            if (nonNull(rLock)) {
                rLock = null;
            }
        }
    }

    public void onLockAcquired() {
        DATABASE_LOG.info("{} Database Lock Acquired", getLockKey());
        Wait.waitALittleMs(5000); // buffer for any lock releasers to finish up remaining writes
        syncQueue();
        if (isNull(queryExecutorPool)) {
            queryExecutorPool = Executors.newSingleThreadScheduledExecutor();
            queryExecutorPool.scheduleWithFixedDelay(this::processQueue, 0L, 250, TimeUnit.MILLISECONDS);
        }
    }

    public void onLockReleased() {
        DATABASE_LOG.info("{} Database Lock Released", getLockKey());
        if (nonNull(queryExecutorPool)) {
            this.queryExecutorPool.shutdownNow();
            this.queryExecutorPool = null;
        }
    }

    /**
     * These lock methods must be executed within the lock thread
     **/

    public boolean hasLock() {
        return rLock.isLocked() && rLock.isHeldByCurrentThread();
    }

    public boolean tryLock() {
        return rLock.tryLock();
    }

    public void releaseLock() {
        if (hasLock()) {
            try {
                redisClient.unlock(rLock);
            } catch (final Exception e) {
                DATABASE_LOG.warn("Error unlocking {} database", getLockKey(), e);
            }
            lockAcquired.set(false);
        }
    }

    public void tryLockProcess() {
        try {
            if (isNull(rLock) || redisClient.isShutDown()) {
                try {
                    rLock = redisClient.getLock(getLockKey());
                } catch (final Exception e) {
                    DATABASE_LOG.error("Failed starting {} database. Unable to initialize lock", getLockKey(), e);
                    stop();
                    return;
                }
            }
            if (isNull(Proxy.getInstance()) || !Proxy.getInstance().isOnlineOn2b2tForAtLeastDuration(Duration.ofSeconds(30))) {
                if (hasLock() || lockAcquired.get()) {
                    releaseLock();
                    onLockReleased();
                }
                return;
            }
            if (!hasLock()) {
                if (tryLock()) {
                    onLockAcquired();
                    lockAcquired.set(true);
                } else {
                    if (lockAcquired.compareAndSet(true, false)) {
                        onLockReleased();
                    }
                }
            } else {
                if (lockAcquired.compareAndSet(false, true)) {
                    onLockAcquired();
                }
            }
        } catch (final Throwable e) {
            DATABASE_LOG.warn("Try lock process exception", e);
        }
    }

    public void insert(final Instant instant, final Query query) {
        final int size = insertQueue.size();
        if (size > maxQueueLen) {
            synchronized (insertQueue) {
                for (int i = 0; i < maxQueueLen / 5; i++) {
                    insertQueue.poll();
                }
            }
        }
        insertQueue.offer(new InsertInstance(instant, query));
    }

    private void processQueue() {
        if (lockAcquired.get() && nonNull(lockExecutorService) && !lockExecutorService.isShutdown()) {
            try {
                final LockingDatabase.InsertInstance insertInstance = insertQueue.poll();
                if (nonNull(insertInstance)) {
                    queryExecutor.execute(insertInstance.getQuery());
                }
            } catch (final Exception e) {
                DATABASE_LOG.error("{} Database queue process exception", getLockKey(), e);
            }
        }
        Wait.waitRandomWithinMsBound(100); // adds some jitter
    }

    @Data
    public static final class InsertInstance {
        private final Instant instant;
        private final Query query;
    }
}
