package com.zenith.database;

import com.zenith.Proxy;
import com.zenith.database.dto.enums.Connectiontype;
import com.zenith.event.client.ClientConnectEvent;
import com.zenith.event.client.ClientDisconnectEvent;
import com.zenith.event.client.ClientOnlineEvent;
import com.zenith.event.db.DatabaseTickEvent;
import com.zenith.event.server.ServerPlayerConnectedEvent;
import com.zenith.event.server.ServerPlayerDisconnectedEvent;
import org.geysermc.mcprotocollib.auth.GameProfile;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;

public class PlaytimeDatabase extends LockingDatabase {
    private final Map<GameProfile, List<ConnectionEvent>> connectionEvents = new ConcurrentHashMap<>();
    private Instant prevSyncTime = Instant.now();

    private record ConnectionEvent(Connectiontype type, long time) {}

    public PlaytimeDatabase(final QueryExecutor queryExecutor, final RedisClient redisClient) {
        super(queryExecutor, redisClient);
    }

    @Override
    public String getLockKey() {
        return "Playtime";
    }

    // return value of this doesn't matter
    // because we are not using the queue based insert system
    @Override
    public Instant getLastEntryTime() {
        try (var handle = this.queryExecutor.jdbi().open()) {
            var result = handle.select("SELECT end_time FROM playtime ORDER BY end_time DESC LIMIT 1;")
                .mapTo(OffsetDateTime.class)
                .findOne();
            if (result.isEmpty()) {
                DATABASE_LOG.warn("Playtime database unable to sync. Database empty?");
                prevSyncTime = Instant.now();
                return prevSyncTime;
            }
            var handoffTime = result.get().toInstant();
            var nowMinusInterval = Instant.now().minusSeconds(DatabaseTickEvent.TICK_INTERVAL_SECONDS);
            if (handoffTime.isAfter(nowMinusInterval)) {
                prevSyncTime = handoffTime;
            } else {
                // we acquired lock sometime after the last playtime write tick
                prevSyncTime = Instant.now();
            }
            return handoffTime;
        }
    }

    @Override
    public void subscribeEvents() {
        resetConnectionEvents();
        EVENT_BUS.subscribe(
            this,
            of(DatabaseTickEvent.class, this::handleDatabaseTick),
            of(ServerPlayerConnectedEvent.class, this::handleServerPlayerConnectedEvent),
            of(ServerPlayerDisconnectedEvent.class, this::handleServerPlayerDisconnectedEvent),
            of(ClientOnlineEvent.class, (e) -> resetConnectionEvents()),
            of(ClientConnectEvent.class, (e) -> resetConnectionEvents()),
            of(ClientDisconnectEvent.class, (e) -> resetConnectionEvents())
        );
    }

    private void resetConnectionEvents() {
        synchronized (this) {
            connectionEvents.clear();
        }
    }

    int i = 0;
    private void handleDatabaseTick(DatabaseTickEvent databaseTickEvent) {
        // tick every 5 minutes instead of 1 minute
        if (i++ % 5 != 0) return;
        synchronized (this) {
            if (Proxy.getInstance().isOn2b2t() && !Proxy.getInstance().isInQueue()) {
                if (this.lockAcquired.get()) {
                    updatePlaytime();
                }
            }
        }
        resetConnectionEvents();
    }

    public void handleServerPlayerConnectedEvent(ServerPlayerConnectedEvent event) {
        if (!Proxy.getInstance().isOn2b2t() || Proxy.getInstance().isInQueue()) return;
        synchronized (this) {
            connectionEvents.compute(event.playerEntry().getProfile(), (k, v) -> {
                if (v == null) v = new ArrayList<>();
                v.add(new ConnectionEvent(Connectiontype.JOIN, Instant.now().getEpochSecond()));
                return v;
            });
        }
    }

    public void handleServerPlayerDisconnectedEvent(ServerPlayerDisconnectedEvent event) {
        if (!Proxy.getInstance().isOn2b2t() || Proxy.getInstance().isInQueue()) return;
        synchronized (this) {
            connectionEvents.compute(event.playerEntry().getProfile(), (k, v) -> {
                if (v == null) v = new ArrayList<>();
                v.add(new ConnectionEvent(Connectiontype.LEAVE, Instant.now().getEpochSecond()));
                return v;
            });
        }
    }

    private void updatePlaytime() {
        var beforeTime = prevSyncTime.atOffset(ZoneOffset.UTC);
        var beforeTimeLong = prevSyncTime.getEpochSecond();
        prevSyncTime = Instant.now();
        var now = Instant.now().atOffset(ZoneOffset.UTC);
        var nowSeconds = now.toEpochSecond();
        var timeDelta = nowSeconds - beforeTimeLong;
        if (timeDelta < 0) {
            DATABASE_LOG.error("playtime interval delta is negative: {} - {} - {}", timeDelta, now.toEpochSecond(), beforeTimeLong);
            return;
        }
        try (var handle = this.queryExecutor.jdbi().open()) {
            handle.inTransaction(transaction -> {
                var tablistEntries = CACHE.getTabListCache().getEntries();
                var batch = transaction.prepareBatch(
                    """
                    INSERT INTO playtime (player_uuid, player_name, playtime_seconds, start_time, end_time) VALUES (:player_uuid, :player_name, :playtime_seconds, :start_time, :end_time);
                    """);
                for (var entry : tablistEntries) {
                    if (connectionEvents.containsKey(entry.getProfile())) continue;
                    // the player has been online since the last db tick
                    var playerUuid = entry.getProfileId();
                    var playerName = entry.getName();
                    batch.bind("player_uuid", playerUuid)
                        .bind("player_name", playerName)
                        .bind("playtime_seconds", timeDelta)
                        .bind("start_time", beforeTime)
                        .bind("end_time", now)
                        .add();
                }
                for (var entry : connectionEvents.entrySet()) {
                    var gameProfile = entry.getKey();
                    var playerUuid = gameProfile.getId();
                    var playerName = gameProfile.getName();
                    var events = entry.getValue();
                    long prevTime = beforeTimeLong;
                    Connectiontype lastType = null;
                    long playtime = 0;
                    for (int i = 0; i < events.size(); i++) {
                        final var event = events.get(i);
                        if (lastType == event.type()) {
                            DATABASE_LOG.error("consecutive events of the same type: {} - {}", lastType, event.type());
                            break;
                        }
                        lastType = event.type();
                        switch (event.type()) {
                            case JOIN -> {
                                if (i+1 < events.size()) {
                                    // there are more events coming up
                                    prevTime = event.time();
                                } else {
                                    if (CACHE.getTabListCache().get(playerUuid).isEmpty()) {
                                        DATABASE_LOG.error("last join event player not in tablist: {} - {}", playerUuid, playerName);
                                        break;
                                    }
                                    // last event and player is currently online
                                    long playtimeDelta = nowSeconds - event.time();
                                    if (playtimeDelta < 0) {
                                        DATABASE_LOG.error("last join playtime delta is negative: {} - {} - {}", playtimeDelta, nowSeconds, prevTime);
                                        break;
                                    }
                                    playtime += playtimeDelta;
                                }
                            }
                            case LEAVE -> {
                                long playtimeDelta = event.time() - prevTime;
                                if (playtimeDelta < 0) {
                                    DATABASE_LOG.error("leave playtime delta is negative: {} - {} - {}", playtimeDelta, nowSeconds, prevTime);
                                    break;
                                }
                                playtime += playtimeDelta;
                            }
                        }
                    }
                    if (playtime > 0) {
                        batch.bind("player_uuid", playerUuid)
                            .bind("player_name", playerName)
                            .bind("playtime_seconds", playtime)
                            .bind("start_time", beforeTime)
                            .bind("end_time", now)
                            .add();
                    }
                }
                return batch.execute();
            });
        }
    }
}
