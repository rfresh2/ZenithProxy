package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoRemovePacket;
import com.zenith.event.proxy.PlayerLogoutInVisualRangeEvent;
import com.zenith.event.proxy.ServerPlayerDisconnectedEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;

import java.util.Optional;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.EVENT_BUS;

public class PlayerInfoRemoveHandler implements ClientEventLoopPacketHandler<ClientboundPlayerInfoRemovePacket, ClientSession> {
    @Override
    public boolean applyAsync(ClientboundPlayerInfoRemovePacket packet, ClientSession session) {
        packet.getProfileIds().forEach(profileId -> {
            Optional<PlayerListEntry> playerEntry = CACHE.getTabListCache().remove(profileId);
            playerEntry.ifPresent(e -> {
                EVENT_BUS.postAsync(new ServerPlayerDisconnectedEvent(e));
                CACHE.getEntityCache().getRecentlyRemovedPlayer(e.getProfileId())
                    .filter(entityPlayer -> !entityPlayer.isSelfPlayer())
                    .ifPresent(entityPlayer -> EVENT_BUS.postAsync(new PlayerLogoutInVisualRangeEvent(e, entityPlayer)));
            });
        });
        return true;
    }
}
