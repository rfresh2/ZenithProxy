package com.zenith.network.client.handler.postoutgoing;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;

import static com.zenith.Shared.CACHE;
import static com.zenith.feature.spectator.SpectatorSync.syncPlayerEquipmentWithSpectatorsFromCache;

public class PostOutgoingContainerClickHandler implements ClientEventLoopPacketHandler<ServerboundContainerClickPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ServerboundContainerClickPacket packet, final ClientSession session) {
        CACHE.getPlayerCache().getInventoryCache().handleContainerClick(packet);
        syncPlayerEquipmentWithSpectatorsFromCache();
        return true;
    }
}
