package com.zenith.database;

import com.collarmc.pounce.Subscribe;
import com.zenith.Proxy;
import com.zenith.cache.data.tab.PlayerEntry;
import com.zenith.database.dto.tables.Chats;
import com.zenith.event.proxy.ServerChatReceivedEvent;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static com.zenith.util.Constants.*;

public class ChatDatabase extends Database {
    public ChatDatabase(ConnectionPool connectionPool) {
        super(connectionPool);
    }

    @Subscribe
    public void handleServerChatReceivedEvent(ServerChatReceivedEvent event) {
        if (!CONFIG.client.server.address.endsWith("2b2t.org") // only write on 2b2t
                || Proxy.getInstance().isInQueue()  // ignore queue
                || !event.message.startsWith("<")) return; // don't write whispers
        try {
            final Optional<PlayerEntry> playerEntry = extractSender(event.message);
            if (playerEntry.isPresent()) {
                final String msg = event.message.substring(event.message.indexOf(">") + 1);
                writeChat(playerEntry.get().getId(), playerEntry.get().getName(), msg, Instant.now().atOffset(ZoneOffset.UTC));
            } else {
                DATABASE_LOG.error("Unable to extract sender for chat message: {}", event.message);
            }
        } catch (final Exception e) {
            DATABASE_LOG.error("Failed handling chat: {}", event.message, e);
        }
    }

    public void writeChat(final UUID playerUUID, final String playerName, final String message, final OffsetDateTime time) {
        try (final Connection connection = connectionPool.getWriteConnection()) {
            final DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);
            final Chats c = Chats.CHATS;
            context.insertInto(c)
                    .set(c.TIME, time)
                    .set(c.CHAT, message)
                    .set(c.PLAYER_UUID, playerUUID)
                    .set(c.PLAYER_NAME, playerName)
                    .execute();
        } catch (final Exception e) {
            if (e.getMessage().contains("violates exclusion constraint") || e.getMessage().contains("deadlock detected")) {
                // expected due to multiple proxies writing the same chat
            } else {
                DATABASE_LOG.error("Failed writing chat {}, {}, {}", playerUUID, playerName, message, e);
            }
        }
    }

    private Optional<PlayerEntry> extractSender(final String message) {
        final String playerName = message.substring(message.indexOf("<") + 1, message.indexOf(">"));
        return CACHE.getTabListCache().getTabList().getFromName(playerName);
    }
}
