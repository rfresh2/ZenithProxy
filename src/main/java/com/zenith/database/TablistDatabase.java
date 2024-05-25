package com.zenith.database;

import com.zenith.event.proxy.DatabaseTickEvent;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static com.zenith.Shared.*;

public class TablistDatabase extends LockingDatabase {
    public TablistDatabase(final QueryExecutor queryExecutor, final RedisClient redisClient) {
        super(queryExecutor, redisClient);
    }

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(this,
            DatabaseTickEvent.class, this::handleTickEvent
        );
    }

    @Override
    public String getLockKey() {
        return "Tablist";
    }

    @Override
    public Instant getLastEntryTime() {
        try (var handle = this.queryExecutor.jdbi().open()) {
            var result = handle.select("SELECT time FROM tablist ORDER BY time DESC LIMIT 1;")
                .mapTo(OffsetDateTime.class)
                .findOne();
            if (result.isEmpty()) {
                DATABASE_LOG.warn("Tablist database unable to sync. Database empty?");
                return Instant.EPOCH;
            }
            return result.get().toInstant();
        }
    }

    private void handleTickEvent(DatabaseTickEvent event) {
        // we aren't using the queue based insert system here so we need to check if we have the lock manually
        if (this.lockAcquired.get()) {
            syncTablist();
        }
    }

    private void syncTablist() {
        try (var handle = this.queryExecutor.jdbi().open()) {
            handle.inTransaction(transaction -> {
                transaction.createUpdate("LOCK TABLE tablist;").execute();
                transaction.createUpdate("DELETE FROM tablist;").execute();
                var batch = transaction.prepareBatch("INSERT INTO tablist (player_name, player_uuid, time) VALUES (:player_name, :player_uuid, :time);");
                for (var entry : CACHE.getTabListCache().getEntries()) {
                    batch
                        .bind("player_name", entry.getName())
                        .bind("player_uuid", entry.getProfileId())
                        .bind("time", Instant.now().atOffset(ZoneOffset.UTC))
                        .add();
                }
                return batch.execute();
            });
        }
    }
}
