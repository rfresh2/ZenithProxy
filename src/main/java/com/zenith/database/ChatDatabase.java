package com.zenith.database;

import com.collarmc.pounce.Subscribe;
import com.zenith.Proxy;
import com.zenith.database.dto.tables.Chats;
import com.zenith.database.dto.tables.records.ChatsRecord;
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

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.DATABASE_LOG;

public class ChatDatabase extends LockingDatabase {
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

    @Subscribe
    public void handleServerChatReceivedEvent(ServerChatReceivedEvent event) {
        if (!CONFIG.client.server.address.endsWith("2b2t.org") // only write on 2b2t
                || Proxy.getInstance().isInQueue()  // ignore queue
                || !event.message.startsWith("<")) return; // don't write whispers or system messages
        try {
            if (event.sender.isPresent()) {
                final String msg = event.message.substring(event.message.indexOf(">") + 2); // skip leading space
                writeChat(event.sender.get().getId(), event.sender.get().getName(), msg, Instant.now().atOffset(ZoneOffset.UTC));
            } else {
                DATABASE_LOG.error("Unable to extract sender for chat message: {}", event.message);
            }
        } catch (final Exception e) {
            DATABASE_LOG.error("Failed handling chat: {}", event.message, e);
        }
    }

    public void writeChat(final UUID playerUUID, final String playerName, final String message, final OffsetDateTime time) {
        final DSLContext context = DSL.using(SQLDialect.POSTGRES);
        final Chats c = Chats.CHATS;
        InsertSetMoreStep<ChatsRecord> query = context.insertInto(c)
                .set(c.TIME, time)
                .set(c.CHAT, message)
                .set(c.PLAYER_UUID, playerUUID)
                .set(c.PLAYER_NAME, playerName);
        this.insert(time.toInstant(), query);
    }
}
