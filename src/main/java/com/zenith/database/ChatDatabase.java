package com.zenith.database;

import com.zenith.Proxy;
import com.zenith.database.dto.records.ChatsRecord;
import com.zenith.event.proxy.ServerChatReceivedEvent;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.zenith.Shared.DATABASE_LOG;
import static com.zenith.Shared.EVENT_BUS;

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
        try (var handle = this.queryExecutor.getJdbi().open()) {
            var result = handle.select("SELECT time FROM chats ORDER BY time DESC LIMIT 1;")
                .mapTo(OffsetDateTime.class)
                .findOne();
            if (result.isEmpty()) {
                DATABASE_LOG.warn("Chats database unable to sync. Database empty?");
                return Instant.EPOCH;
            }
            return result.get().toInstant();
        }
    }

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(this,
            ServerChatReceivedEvent.class, this::handleServerChatReceivedEvent
        );
    }


    public void handleServerChatReceivedEvent(ServerChatReceivedEvent event) {
        if (!Proxy.getInstance().isOn2b2t() // only write on 2b2t
                || Proxy.getInstance().isInQueue()  // ignore queue
                || !event.isPublicChat()) return; // don't write whispers or system messages
        try {
            if (event.sender().isPresent()) {
                writeChat(event.sender().get().getProfileId(), event.sender().get().getName(), event.publicChatContent(), Instant.now().atOffset(ZoneOffset.UTC));
            } else {
                DATABASE_LOG.error("Unable to extract sender for chat message: {}", event.message());
            }
        } catch (final Exception e) {
            DATABASE_LOG.error("Failed handling chat: {}", event.message(), e);
        }
    }

    public void writeChat(final UUID playerUuid, final String playerName, final String chat, final OffsetDateTime time) {
        this.insert(time.toInstant(), new ChatsRecord(time, chat, playerName, playerUuid), handle ->
            handle.createUpdate("INSERT INTO chats (time, chat, player_name, player_uuid) VALUES (:time, :chat, :player_name, :player_uuid);")
                .bind("time", time)
                .bind("chat", chat)
                .bind("player_name", playerName)
                .bind("player_uuid", playerUuid)
                .execute());
    }
}
