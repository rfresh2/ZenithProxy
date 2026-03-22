package com.zenith.database;

import com.zenith.event.db.DatabaseTickEvent;
import com.zenith.feature.player.World;
import com.zenith.mc.dimension.DimensionRegistry;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;

public class TimeDatabase extends LockingDatabase {
    public TimeDatabase(final QueryExecutor queryExecutor, final RedisClient redisClient) {
        super(queryExecutor, redisClient);
    }

    @Override
    public String getLockKey() {
        return "Time";
    }

    @Override
    public Instant getLastEntryTime() {
        try (var handle = this.queryExecutor.jdbi().open()) {
            var result = handle.select("SELECT time from worldtime ORDER BY time DESC LIMIT 1")
                .mapTo(OffsetDateTime.class)
                .findOne();
            if (result.isEmpty()) {
                DATABASE_LOG.warn("Time database unable to sync. Database empty?");
                return Instant.EPOCH;
            }
            return result.get().toInstant();
        }
    }

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(
            this,
            of(DatabaseTickEvent.class, this::handleDatabaseTick)
        );
    }

    @Override
    public boolean tryLock() {
        if (World.getCurrentDimension() != DimensionRegistry.OVERWORLD.get()) return false;
        return super.tryLock();
    }

    private void handleDatabaseTick(DatabaseTickEvent event) {
        if (World.getCurrentDimension() != DimensionRegistry.OVERWORLD.get()) {
            if (lockAcquired.get()) {
                try {
                    lockExecutorService.submit(() -> {
                        if (hasLock() || lockAcquired.get()) {
                            releaseLock();
                            onLockReleased();
                        }
                    }, true).get(5, TimeUnit.SECONDS);
                } catch (final Exception e) {
                    DATABASE_LOG.warn("Failed releasing lock", e);
                }
            }
            return;
        }
        var worldTimeData = CACHE.getChunkCache().getWorldTimeData();
        if (worldTimeData == null) return;
        // cached worldtime data is updated in-place
        // technically there is a possible race condition here
        // but should be pretty unlikely if we extract the data out quickly here
        var timeInstant = Instant.ofEpochMilli(worldTimeData.getLastUpdate());
        var time = timeInstant.atOffset(ZoneOffset.UTC);
        long worldage = worldTimeData.getGameTime();
        long worldtime = 0L; // todo: worldTimeData.isTickDayTime() ? worldTimeData.getDayTime() : -worldTimeData.getDayTime();
        insert(timeInstant, handle -> {
            handle.createUpdate("INSERT INTO worldtime (time, worldage, worldtime) VALUES  (:time, :worldage, :worldtime)")
                  .bind("time", time)
                  .bind("worldage", worldage)
                  .bind("worldtime", worldtime)
                  .execute();
        });
    }
}
