package com.zenith.database;

import com.zenith.Proxy;
import com.zenith.database.dto.enums.Connectiontype;
import com.zenith.database.dto.records.ConnectionsRecord;
import com.zenith.event.proxy.DisconnectEvent;
import com.zenith.event.proxy.ServerPlayerConnectedEvent;
import com.zenith.event.proxy.ServerPlayerDisconnectedEvent;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;

public class ConnectionsDatabase extends LiveDatabase {
    public ConnectionsDatabase(final QueryExecutor queryExecutor, final RedisClient redisClient) {
        super(queryExecutor, redisClient);
    }

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(
            this,
            of(ServerPlayerConnectedEvent.class, this::handleServerPlayerConnectedEvent),
            of(ServerPlayerDisconnectedEvent.class, this::handleServerPlayerDisconnectedEvent),
            of(DisconnectEvent.class, 10, this::handleDisconnectEvent) // higher priority before cache is reset
        );
    }

    @Override
    public String getLockKey() {
        return "Connections";
    }

    @Override
    public Instant getLastEntryTime() {
        try (var handle = this.queryExecutor.jdbi().open()) {
            var result = handle.select("SELECT time FROM connections ORDER BY time DESC LIMIT 1;")
                .mapTo(OffsetDateTime.class)
                .findOne();
            if (result.isEmpty()) {
                DATABASE_LOG.warn("Connections database unable to sync. Database empty?");
                return Instant.EPOCH;
            }
            return result.get().toInstant();
        }
    }

    @Override
    public int getMaxQueueLength() {
        return 300; // higher limit needed here to handle restarts where there are mass disconnects/connects
    }

    public void handleServerPlayerConnectedEvent(ServerPlayerConnectedEvent event) {
        writeConnection(Connectiontype.JOIN, event.playerEntry().getName(), event.playerEntry().getProfileId(), Instant.now().atOffset(ZoneOffset.UTC));
    }

    public void handleServerPlayerDisconnectedEvent(ServerPlayerDisconnectedEvent event) {
        writeConnection(Connectiontype.LEAVE, event.playerEntry().getName(), event.playerEntry().getProfileId(), Instant.now().atOffset(ZoneOffset.UTC));
    }

    public void handleDisconnectEvent(DisconnectEvent event) {
        if (!Proxy.getInstance().isOn2b2t()
            || event.wasInQueue()
            || event.onlineDuration().toMinutes() < 15) return;
        var profile = CACHE.getProfileCache().getProfile();
        if (profile == null || profile.getName() == null || profile.getId() == null) return;
        writeConnection(Connectiontype.LEAVE, profile.getName(), profile.getId(), Instant.now().atOffset(ZoneOffset.UTC));
    }

    // todo: handle server restart
    //  non-prio proxies will proactively disconnect
    //  so we should just mark everyone online as disconnected
    //  we will need to ensure we handle deduplication properly for this situation
    //  i.e. a prio instance which does not proactively disconnect will write timestamps for the disconnects that are wildly different, bypassing the dedupe constraints in the db
    //  could cause duplicated data, once from the proactive marking as disconnected, and once on actual restart disconnects
    //  need to think about a better approach for this

    public void writeConnection(final Connectiontype connectiontype, final String playerName, final UUID playerUUID, final OffsetDateTime time) {
        if (!Proxy.getInstance().isOnlineOn2b2tForAtLeastDuration(Duration.ofSeconds(3))) return;
        insert(time.toInstant(), new ConnectionsRecord(time, connectiontype, playerName, playerUUID), handle ->
            handle.createUpdate("INSERT INTO connections (time, connection, player_name, player_uuid) VALUES (:time, :connection, :playerName, :playerUuid)")
                .bind("time", time)
                .bind("connection", connectiontype)
                .bind("playerName", playerName)
                .bind("playerUuid", playerUUID)
                .execute());
    }
}
