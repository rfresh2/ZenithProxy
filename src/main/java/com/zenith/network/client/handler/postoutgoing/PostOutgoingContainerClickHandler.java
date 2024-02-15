package com.zenith.network.client.handler.postoutgoing;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PostOutgoingPacketHandler;

import static com.zenith.Shared.CACHE;
import static com.zenith.feature.spectator.SpectatorUtils.syncPlayerEquipmentWithSpectatorsFromCache;

public class PostOutgoingContainerClickHandler implements PostOutgoingPacketHandler<ServerboundContainerClickPacket, ClientSession> {
    @Override
    public void accept(final ServerboundContainerClickPacket packet, final ClientSession session) {
        CACHE.getPlayerCache().getInventoryCache().handleContainerClick(packet);
        syncPlayerEquipmentWithSpectatorsFromCache();
    }
}
