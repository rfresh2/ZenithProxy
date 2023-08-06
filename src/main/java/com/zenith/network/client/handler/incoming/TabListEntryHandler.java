package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListEntryPacket;
import com.zenith.cache.data.tab.PlayerEntry;
import com.zenith.event.proxy.ServerPlayerConnectedEvent;
import com.zenith.event.proxy.ServerPlayerDisconnectedEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.function.Consumer;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.EVENT_BUS;
import static java.util.Objects.nonNull;

public class TabListEntryHandler implements AsyncIncomingHandler<ServerPlayerListEntryPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerPlayerListEntryPacket packet, @NonNull ClientSession session) {
        Consumer<PlayerListEntry> consumer = null;
        switch (packet.getAction()) {
            case ADD_PLAYER:
                consumer = entry -> {
                    CACHE.getTabListCache().getTabList().add(entry);
                    // prevent mass spam on initial join
                    if (session.getProxy().getConnectTime().isBefore(Instant.now().minus(3L, ChronoUnit.SECONDS))) {
                        EVENT_BUS.dispatch(new ServerPlayerConnectedEvent(CACHE.getTabListCache().getTabList().get(entry)));
                    }
                };
                break;
            case REMOVE_PLAYER:
                consumer = entry -> {
                    Optional<PlayerEntry> playerEntry = CACHE.getTabListCache().getTabList().remove(entry);
                    playerEntry.ifPresent(e -> EVENT_BUS.dispatch(new ServerPlayerDisconnectedEvent(e)));
                };
                break;
        }
        if (nonNull(consumer)) {
            for (PlayerListEntry entry : packet.getEntries()) {
                consumer.accept(entry);
            }
        }
        return true;
    }

    @Override
    public Class<ServerPlayerListEntryPacket> getPacketClass() {
        return ServerPlayerListEntryPacket.class;
    }
}
