package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoUpdatePacket;
import com.zenith.event.proxy.ServerPlayerConnectedEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;
import lombok.NonNull;

import static com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction.*;
import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.EVENT_BUS;

public class PlayerInfoUpdateHandler implements AsyncPacketHandler<ClientboundPlayerInfoUpdatePacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundPlayerInfoUpdatePacket packet, @NonNull ClientSession session) {
        if (packet.getActions().contains(ADD_PLAYER)) {
            for (var entry : packet.getEntries()) {
                CACHE.getTabListCache().add(entry);
                EVENT_BUS.postAsync(new ServerPlayerConnectedEvent(entry));
            }
        } else {
            for (var newEntry : packet.getEntries()) {
                CACHE.getTabListCache().get(newEntry.getProfileId()).ifPresent(entry -> {
                    if (packet.getActions().contains(INITIALIZE_CHAT)) {
                        entry.setSessionId(newEntry.getSessionId());
                        entry.setExpiresAt(newEntry.getExpiresAt());
                        entry.setKeySignature(newEntry.getKeySignature());
                        entry.setPublicKey(newEntry.getPublicKey());
                    }

                    if (packet.getActions().contains(UPDATE_GAME_MODE)) {
                        entry.setGameMode(newEntry.getGameMode());
                    }

                    if (packet.getActions().contains(UPDATE_LISTED)) {
                        entry.setListed(newEntry.isListed());
                    }

                    if (packet.getActions().contains(UPDATE_LATENCY)) {
                        entry.setLatency(newEntry.getLatency());
                    }

                    if (packet.getActions().contains(UPDATE_DISPLAY_NAME)) {
                        entry.setDisplayName(newEntry.getDisplayName());
                    }
                });
            }
        }
        return true;
    }
}
