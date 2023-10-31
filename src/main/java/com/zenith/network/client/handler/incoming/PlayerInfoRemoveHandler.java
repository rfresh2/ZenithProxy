package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoRemovePacket;
import com.zenith.event.proxy.PlayerLogoutInVisualRangeEvent;
import com.zenith.event.proxy.ServerPlayerDisconnectedEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;

import java.util.Objects;
import java.util.Optional;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.EVENT_BUS;

public class PlayerInfoRemoveHandler implements AsyncIncomingHandler<ClientboundPlayerInfoRemovePacket, ClientSession> {
    @Override
    public boolean applyAsync(ClientboundPlayerInfoRemovePacket packet, ClientSession session) {
        packet.getProfileIds().forEach(profileId -> {
            Optional<PlayerListEntry> playerEntry = CACHE.getTabListCache().remove(profileId);
            playerEntry.ifPresent(e -> {
                EVENT_BUS.postAsync(new ServerPlayerDisconnectedEvent(e));
                GameProfile currentPlayerProfile = CACHE.getProfileCache().getProfile();
                if (currentPlayerProfile == null || Objects.equals(e.getProfileId(), currentPlayerProfile.getId())) return;
                CACHE.getEntityCache().getRecentlyRemovedPlayer(e.getProfileId())
                    .ifPresent(entityPlayer -> EVENT_BUS.postAsync(new PlayerLogoutInVisualRangeEvent(e, entityPlayer)));
            });
        });
        return true;
    }
}
