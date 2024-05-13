package com.zenith.network.client.handler.postoutgoing;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClosePacket;

import static com.zenith.Shared.CACHE;
import static com.zenith.feature.spectator.SpectatorSync.syncPlayerEquipmentWithSpectatorsFromCache;

public class PostOutgoingContainerCloseHandler implements ClientEventLoopPacketHandler<ServerboundContainerClosePacket, ClientSession> {
    @Override
    public boolean applyAsync(final ServerboundContainerClosePacket packet, final ClientSession session) {
        CACHE.getPlayerCache().closeContainer(packet.getContainerId());
        syncPlayerEquipmentWithSpectatorsFromCache();
        return true;
    }
}
