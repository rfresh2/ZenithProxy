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
        for (int i = 0; i < packet.getEntries().length; i++) {
            var entry = packet.getEntries()[i];
            if (packet.getActions().contains(ADD_PLAYER)) {
                CACHE.getTabListCache().add(entry);
                EVENT_BUS.postAsync(new ServerPlayerConnectedEvent(entry));
            }
            // skip extra ops if we're only adding a player
            if (packet.getActions().size() <= 1 && packet.getActions().contains(ADD_PLAYER)) continue;
            CACHE.getTabListCache().get(entry.getProfileId()).ifPresent(e -> {
                if (packet.getActions().contains(INITIALIZE_CHAT)) {
                    e.setSessionId(entry.getSessionId());
                    e.setExpiresAt(entry.getExpiresAt());
                    e.setKeySignature(entry.getKeySignature());
                    e.setPublicKey(entry.getPublicKey());
                }
                if (packet.getActions().contains(UPDATE_GAME_MODE))
                    e.setGameMode(entry.getGameMode());
                if (packet.getActions().contains(UPDATE_LISTED))
                    e.setListed(entry.isListed());
                if (packet.getActions().contains(UPDATE_LATENCY))
                    e.setLatency(entry.getLatency());
                if (packet.getActions().contains(UPDATE_DISPLAY_NAME))
                    e.setDisplayName(entry.getDisplayName());
            });
        }
        return true;
    }
}
