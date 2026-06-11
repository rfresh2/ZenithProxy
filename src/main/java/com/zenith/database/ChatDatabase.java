package com.zenith.database;

import com.zenith.Proxy;
import com.zenith.database.dto.records.ChatsRecord;
import com.zenith.event.chat.PublicChatEvent;
import com.zenith.util.ComponentSerializer;
import net.kyori.adventure.text.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.DATABASE_LOG;
import static com.zenith.Globals.EVENT_BUS;

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
        try (var handle = this.queryExecutor.jdbi().open()) {
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
        EVENT_BUS.subscribe(
            this,
            of(PublicChatEvent.class, this::handlePublicChatEvent)
        );
    }

    private void handlePublicChatEvent(PublicChatEvent event) {
        if (!Proxy.getInstance().isOn2b2t() // only write on 2b2t
            || Proxy.getInstance().isInQueue()) return;  // ignore queue
        try {
            writeChat(event.sender().getProfileId(), event.sender().getName(), event.message(), Instant.now().atOffset(ZoneOffset.UTC), event.component());
        } catch (final Exception e) {
            DATABASE_LOG.error("Failed handling chat: {}", event.message(), e);
        }
    }

    public void writeChat(final UUID playerUuid, final String playerName, final String chat, final OffsetDateTime time, final Component component) {
        this.insert(time.toInstant(), new ChatsRecord(time, chat, playerName, playerUuid, ComponentSerializer.serializeJson(component)), handle ->
            handle.createUpdate("INSERT INTO chats (time, chat, player_name, player_uuid) VALUES (:time, :chat, :player_name, :player_uuid);")
                .bind("time", time)
                .bind("chat", chat)
                .bind("player_name", playerName)
                .bind("player_uuid", playerUuid)
                .execute());
    }
}
