package com.zenith.database;

import com.collarmc.pounce.Subscribe;
import com.zenith.Proxy;
import com.zenith.database.dto.enums.Connectiontype;
import com.zenith.database.dto.tables.Connections;
import com.zenith.database.dto.tables.records.ConnectionsRecord;
import com.zenith.event.proxy.ServerPlayerConnectedEvent;
import com.zenith.event.proxy.ServerPlayerDisconnectedEvent;
import org.jooq.DSLContext;
import org.jooq.InsertSetMoreStep;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.zenith.util.Constants.CONFIG;

public class ConnectionsDatabase extends Database {
    public ConnectionsDatabase(QueryQueue queryQueue) {
        super(queryQueue);
    }

    @Subscribe
    public void handleServerPlayerConnectedEvent(ServerPlayerConnectedEvent event) {
        writeConnection(Connectiontype.JOIN, event.playerEntry.getName(), event.playerEntry.getId(), Instant.now().atOffset(ZoneOffset.UTC));
    }

    @Subscribe
    public void handleServerPlayerDisconnectedEvent(ServerPlayerDisconnectedEvent event) {
        writeConnection(Connectiontype.LEAVE, event.playerEntry.getName(), event.playerEntry.getId(), Instant.now().atOffset(ZoneOffset.UTC));
    }

    // todo: handle server restart
    //  non-prio proxies will proactively disconnect
    //  so we should just mark everyone online as disconnected
    //  we will need to ensure we handle deduplication properly for this situation
    //  i.e. a prio instance which does not proactively disconnect will write timestamps for the disconnects that are wildly different, bypassing the dedupe constraints in the db
    //  could cause duplicated data, once from the proactive marking as disconnected, and once on actual restart disconnects
    //  need to think about a better approach for this

    public void writeConnection(final Connectiontype connectiontype, final String playerName, final UUID playerUUID, final OffsetDateTime time) {
        if (!CONFIG.client.server.address.endsWith("2b2t.org")
                || Proxy.getInstance().isInQueue()
                || (Proxy.getInstance().getConnectTime().isBefore(Instant.now().minus(Duration.ofSeconds(3))) && playerName.equals(CONFIG.authentication.username))
        ) return;
        final DSLContext context = DSL.using(SQLDialect.POSTGRES);
        final Connections c = Connections.CONNECTIONS;
        InsertSetMoreStep<ConnectionsRecord> query = context.insertInto(c)
                .set(c.TIME, time)
                .set(c.CONNECTION, connectiontype)
                .set(c.PLAYER_NAME, playerName)
                .set(c.PLAYER_UUID, playerUUID);
        queryQueue.add(query);
    }
}
