package com.zenith.database;

import com.zenith.event.Subscription;
import com.zenith.event.proxy.DatabaseTickEvent;
import com.zenith.feature.queue.Queue;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static com.zenith.Shared.DATABASE_LOG;
import static com.zenith.Shared.EVENT_BUS;

public class QueueLengthDatabase extends LockingDatabase {
    public QueueLengthDatabase(QueryExecutor queryExecutor, RedisClient redisClient) {
        super(queryExecutor, redisClient);
    }

    @Override
    public Subscription subscribeEvents() {
        return EVENT_BUS.subscribe(
            DatabaseTickEvent.class, this::handleTickEvent
        );
    }

    @Override
    public String getLockKey() {
        return "QueueLength";
    }

    @Override
    public Instant getLastEntryTime() {
        try (var handle = this.queryExecutor.getJdbi().open()) {
            var result = handle.select("SELECT time FROM queuelength ORDER BY time DESC LIMIT 1;")
                .mapTo(OffsetDateTime.class)
                .findOne();
            if (result.isEmpty()) {
                DATABASE_LOG.warn("QueueLength database unable to sync. Database empty?");
                return Instant.EPOCH;
            }
            return result.get().toInstant();
        }
    }

    public void handleTickEvent(final DatabaseTickEvent event) {
        var queueStatus = Queue.getQueueStatus();
        this.insert(Instant.now(), handle ->
            handle.createUpdate("INSERT INTO queuelength (time, regular, prio) VALUES (:time, :regular, :prio)")
                .bind("time", Instant.now().atOffset(ZoneOffset.UTC))
                .bind("regular", (short) queueStatus.regular())
                .bind("prio", (short) queueStatus.prio())
                .execute());
    }
}
