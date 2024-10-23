package com.zenith.database;

import com.zenith.Proxy;
import com.zenith.event.proxy.RedisRestartEvent;
import com.zenith.util.Wait;
import org.jdbi.v3.core.HandleConsumer;
import org.redisson.RedissonShutdownException;
import org.redisson.api.RLock;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Base class for databases that require a lock to be acquired before writing
 */
public abstract class LockingDatabase extends Database {
    private static final int defaultMaxQueueLen = 100;
    protected final Queue<InsertInstance> insertQueue = new ConcurrentLinkedQueue<>();
    protected final AtomicBoolean lockAcquired = new AtomicBoolean(false);
    protected final RedisClient redisClient;
    private RLock rLock;
    protected ScheduledExecutorService lockExecutorService;
    private ScheduledFuture<?> queryExecutorFuture;
    private final AtomicBoolean redisRestarted = new AtomicBoolean(false);
    private final Object eventListener = new Object();

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

    public int getMaxQueueLength() {
        return lockAcquired.get() ? 500 : defaultMaxQueueLen;
    }

    /**
     * Sync the current insert queue with what is in the remote database
     * intended as a deduping mechanism
     */
    public void syncQueue() {
        try {
            final long lastRecordSeenTimeEpochMs = getLastEntryTime().toEpochMilli();
            synchronized (this.insertQueue) {
                while (nonNull(this.insertQueue.peek()) && this.insertQueue.peek().instant().toEpochMilli()
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
        if (this.isRunning) return;
        super.start();
        EVENT_BUS.subscribe(eventListener, of(RedisRestartEvent.class, e -> redisRestarted.set(true)));
        this.lockAcquired.set(false);
        synchronized (this) {
            if (isNull(lockExecutorService)) {
                lockExecutorService = Executors.newSingleThreadScheduledExecutor();
            }
        }
        lockExecutorService.scheduleAtFixedRate(this::tryLockProcess, (long) (Math.random() * 10), 10L, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        super.stop();
        synchronized (this) {
            EVENT_BUS.unsubscribe(eventListener);
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
        writeLockInfo();
        Wait.wait(20); // buffer for any lock releasers to finish up remaining writes
        syncQueue();
        if (isNull(queryExecutorFuture) || queryExecutorFuture.isDone()) {
            queryExecutorFuture = EXECUTOR.scheduleWithFixedDelay(this::processQueue, 0L, 50, TimeUnit.MILLISECONDS);
        }
    }

    public void onLockReleased() {
        DATABASE_LOG.info("{} Database Lock Released", getLockKey());
        if (nonNull(queryExecutorFuture)) {
            queryExecutorFuture.cancel(true);
            while (!queryExecutorFuture.isDone()) {
                Wait.waitMs(50);
            }
        }
    }

    public void writeLockInfo() {
        try {
            this.redisClient.getRedissonClient()
                .getBucket(getLockKey() + "_lock_info")
                .set(
                    "Player=" + CONFIG.authentication.username + ", " +
                    "IP=" + CONFIG.server.proxyIP + ", " +
                    "Time=" + Instant.now().toString() + ", " +
                    "Version=" + LAUNCH_CONFIG.version
                );
        } catch (final Exception e) {
            DATABASE_LOG.warn("Error writing lock info for database: {}", getLockKey(), e);
        }
    }

    /**
     * These lock methods must be executed within the lock thread
     **/

    public boolean hasLock() {
        try {
            return rLock.isLocked() && rLock.isHeldByCurrentThread();
        } catch (final Exception e) {
            return false;
        }
    }

    public boolean tryLock() {
        return rLock.tryLock();
    }

    public void releaseLock() {
        try {
            redisClient.unlock(rLock);
        } catch (final Exception e) {
            DATABASE_LOG.warn("Error unlocking {} database", getLockKey(), e);
        }
        lockAcquired.set(false);
    }

    public void tryLockProcess() {
        try {
            if (redisRestarted.getAndSet(false)) {
                releaseLock();
                onLockReleased();
                rLock = null;
            }
            if (rLock == null || redisClient.isShutDown()) {
                try {
                    rLock = redisClient.getLock(getLockKey());
                } catch (final Exception e) {
                    DATABASE_LOG.error("Failed starting {} database. Unable to initialize lock", getLockKey(), e);
                    stop();
                    return;
                }
            }
            if (!Proxy.getInstance().isOnlineOn2b2tForAtLeastDuration(Duration.ofSeconds(30))) {
                if (hasLock() || lockAcquired.get()) {
                    // 5 second grace period to finish up remaining inserts
                    if (insertQueue.isEmpty()
                        || Proxy.getInstance().getDisconnectTime().isBefore(Instant.now().minusSeconds(5))) {
                        releaseLock();
                        onLockReleased();
                    }
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
            try {
                releaseLock();
                onLockReleased();
            } catch (final Exception e2) {
                DATABASE_LOG.error("Error releasing lock in try lock process exception", e2);
            }
            if (e instanceof RedissonShutdownException) {
                redisClient.restart();
            }
            Wait.wait(30);
        }
    }

    public void insert(final Instant instant, final HandleConsumer query) {
        insert(instant, null, query);
    }

    protected void insert(final Instant instant, final Runnable liveRunnable, final HandleConsumer query) {
        if (insertQueue.size() > getMaxQueueLength()) {
            if (lockAcquired.get()) {
                DATABASE_LOG.warn("Insert queue size: {} > {} : Flushing {} entries in DB: {}", insertQueue.size(), getMaxQueueLength(), insertQueue.size() - getMaxQueueLength(), getLockKey());
            }
            synchronized (insertQueue) {
                while (insertQueue.size() > getMaxQueueLength() / 2) {
                    insertQueue.poll();
                }
            }
        }
        insertQueue.offer(new InsertInstance(instant, liveRunnable, query));
    }

    protected void processQueue() {
        if (lockAcquired.get() && nonNull(lockExecutorService) && !lockExecutorService.isShutdown()) {
            try {
                final LockingDatabase.InsertInstance insertInstance = insertQueue.poll();
                if (insertInstance != null) {
                    queryExecutor.execute(insertInstance::query);
                    if (nonNull(insertInstance.redisQuery())) {
                        EXECUTOR.execute(() -> insertInstance.redisQuery().run());
                    }
                }
            } catch (final Exception e) {
                DATABASE_LOG.error("{} Database queue process exception", getLockKey(), e);
            }
        }
        Wait.waitRandomMs(50); // adds some jitter
    }


    public record InsertInstance(Instant instant, @Nullable Runnable redisQuery, HandleConsumer query) { }
}
