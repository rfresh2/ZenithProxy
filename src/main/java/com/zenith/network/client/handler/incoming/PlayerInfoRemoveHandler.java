package com.zenith.network.client.handler.incoming;

import com.zenith.event.proxy.PlayerLogoutInVisualRangeEvent;
import com.zenith.event.proxy.ServerPlayerDisconnectedEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerInfoRemovePacket;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.EVENT_BUS;

public class PlayerInfoRemoveHandler implements ClientEventLoopPacketHandler<ClientboundPlayerInfoRemovePacket, ClientSession> {
    @Override
    public boolean applyAsync(ClientboundPlayerInfoRemovePacket packet, ClientSession session) {
        List<UUID> profileIds = packet.getProfileIds();
        for (int i = 0; i < profileIds.size(); i++) {
            final UUID profileId = profileIds.get(i);
            Optional<PlayerListEntry> playerEntry = CACHE.getTabListCache().remove(profileId);
            playerEntry.ifPresent(e -> {
                EVENT_BUS.postAsync(new ServerPlayerDisconnectedEvent(e));
                CACHE.getEntityCache().getRecentlyRemovedPlayer(e.getProfileId())
                    .filter(entityPlayer -> !entityPlayer.isSelfPlayer())
                    .ifPresent(entityPlayer -> EVENT_BUS.postAsync(new PlayerLogoutInVisualRangeEvent(e, entityPlayer)));
            });
        }
        return true;
    }
}
