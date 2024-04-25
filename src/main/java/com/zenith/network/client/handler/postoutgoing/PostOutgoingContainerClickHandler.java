package com.zenith.network.client.handler.postoutgoing;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PostOutgoingPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;

import static com.zenith.Shared.CACHE;
import static com.zenith.feature.spectator.SpectatorSync.syncPlayerEquipmentWithSpectatorsFromCache;

public class PostOutgoingContainerClickHandler implements PostOutgoingPacketHandler<ServerboundContainerClickPacket, ClientSession> {
    @Override
    public void accept(final ServerboundContainerClickPacket packet, final ClientSession session) {
        CACHE.getPlayerCache().getInventoryCache().handleContainerClick(packet);
        syncPlayerEquipmentWithSpectatorsFromCache();
    }
}
