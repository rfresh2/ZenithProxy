package com.zenith.database;

import com.zenith.event.proxy.ServerRestartingEvent;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static com.zenith.Shared.DATABASE_LOG;
import static com.zenith.Shared.EVENT_BUS;

public class RestartsDatabase extends LockingDatabase {

    private static final Duration cooldownDuration = Duration.ofMinutes(20L);
    private Instant lastRestartWrite = Instant.EPOCH;

    public RestartsDatabase(QueryExecutor queryExecutor, RedisClient redisClient) {
        super(queryExecutor, redisClient);
    }

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(this,
                ServerRestartingEvent.class, this::handleServerRestartEvent
        );
    }

    @Override
    public String getLockKey() {
        return "Restarts";
    }

    @Override
    public Instant getLastEntryTime() {
        try (var handle = this.queryExecutor.getJdbi().open()) {
            var result = handle.select("SELECT time FROM restarts ORDER BY time DESC LIMIT 1;")
                    .mapTo(OffsetDateTime.class)
                    .findOne();
            if (result.isEmpty()) {
                DATABASE_LOG.warn("Restarts database unable to sync. Database empty?");
                return Instant.EPOCH;
            }
            return result.get().toInstant();
        }
    }

    public void handleServerRestartEvent(final ServerRestartingEvent event) {
        synchronized (this) {
            if (lastRestartWrite.isBefore(Instant.now().minus(cooldownDuration))) {
                lastRestartWrite = Instant.now();
                this.insert(Instant.now(), handle ->
                    handle.createUpdate("INSERT INTO restarts (time) VALUES (:time)")
                        .bind("time", Instant.now().plus(Duration.ofMinutes(15L)).atOffset(ZoneOffset.UTC))
                        .execute());
            }
        }
    }
}
