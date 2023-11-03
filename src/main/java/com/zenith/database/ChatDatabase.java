package com.zenith.database;

import com.zenith.Proxy;
import com.zenith.database.dto.tables.Chats;
import com.zenith.database.dto.tables.records.ChatsRecord;
import com.zenith.event.Subscription;
import com.zenith.event.proxy.ServerChatReceivedEvent;
import org.jooq.DSLContext;
import org.jooq.InsertSetMoreStep;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.zenith.Shared.*;

public class ChatDatabase extends LiveDatabase {
    public ChatDatabase(QueryExecutor queryExecutor, RedisClient redisClient) {
        super(queryExecutor, redisClient);
    }

    @Override
    public String getLockKey() {
        return "Chats";
    }

    @Override
    public Instant getLastEntryTime() {
        final DSLContext context = DSL.using(SQLDialect.POSTGRES);
        final Chats c = Chats.CHATS;
        final Result<ChatsRecord> recordResult = this.queryExecutor.fetch(context.selectFrom(c)
                .orderBy(c.TIME.desc())
                .limit(1));
        if (recordResult.isEmpty()) {
            DATABASE_LOG.warn("Chats database unable to sync. Database empty?");
            return Instant.EPOCH;
        }
        final ChatsRecord chatsRecord = recordResult.get(0);
        return chatsRecord.get(c.TIME).toInstant();
    }

    @Override
    public Subscription subscribeEvents() {
        return EVENT_BUS.subscribe(
            ServerChatReceivedEvent.class, this::handleServerChatReceivedEvent
        );
    }


    public void handleServerChatReceivedEvent(ServerChatReceivedEvent event) {
        if (!CONFIG.client.server.address.endsWith("2b2t.org") // only write on 2b2t
                || Proxy.getInstance().isInQueue()  // ignore queue
                || !event.message().startsWith("<")) return; // don't write whispers or system messages
        try {
            if (event.sender().isPresent()) {
                final String msg = event.message().substring(event.message().indexOf(">") + 2); // skip leading space
                writeChat(event.sender().get().getProfileId(), event.sender().get().getName(), msg, Instant.now().atOffset(ZoneOffset.UTC));
            } else {
                DATABASE_LOG.error("Unable to extract sender for chat message: {}", event.message());
            }
        } catch (final Exception e) {
            DATABASE_LOG.error("Failed handling chat: {}", event.message(), e);
        }
    }

    public void writeChat(final UUID playerUUID, final String playerName, final String message, final OffsetDateTime time) {
        final DSLContext context = DSL.using(SQLDialect.POSTGRES);
        final Chats c = Chats.CHATS;
        var record = context.newRecord(c)
            .setTime(time)
            .setChat(message)
            .setPlayerUuid(playerUUID)
            .setPlayerName(playerName);
        InsertSetMoreStep<ChatsRecord> query = context.insertInto(c)
                .set(record);
        this.insert(time.toInstant(), record.into(com.zenith.database.dto.tables.pojos.Chats.class), query);
    }
}
