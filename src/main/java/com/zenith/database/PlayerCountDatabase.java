package com.zenith.database;

import com.zenith.event.proxy.DatabaseTickEvent;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static com.zenith.Shared.*;

public class PlayerCountDatabase extends LockingDatabase {

    public PlayerCountDatabase(QueryExecutor queryExecutor, RedisClient redisClient) {
        super(queryExecutor, redisClient);
    }

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(this,
            DatabaseTickEvent.class, this::handleDatabaseTickEvent
        );
    }

    @Override
    public String getLockKey() {
        return "PlayerCount";
    }

    @Override
    public Instant getLastEntryTime() {
        try (var handle = this.queryExecutor.jdbi().open()) {
            var result = handle.select("SELECT time FROM playercount ORDER BY time DESC LIMIT 1;")
                .mapTo(OffsetDateTime.class)
                .findOne();
            if (result.isEmpty()) {
                DATABASE_LOG.warn("Player Count database unable to sync. Database empty?");
                return Instant.EPOCH;
            }
            return result.get().toInstant();
        }
    }

    public void handleDatabaseTickEvent(final DatabaseTickEvent event) {
        this.insert(Instant.now(), handle ->
            handle.createUpdate("INSERT INTO playercount (time, count) VALUES (:time, :count)")
                .bind("time", Instant.now().atOffset(ZoneOffset.UTC))
                .bind("count", (short) CACHE.getTabListCache().getEntries().size())
                .execute());
    }
}
