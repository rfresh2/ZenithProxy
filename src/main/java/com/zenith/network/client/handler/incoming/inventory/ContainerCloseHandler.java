package com.zenith.network.client.handler.incoming.inventory;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerClosePacket;

import static com.zenith.Shared.CACHE;
import static com.zenith.feature.spectator.SpectatorSync.syncPlayerEquipmentWithSpectatorsFromCache;

public class ContainerCloseHandler implements ClientEventLoopPacketHandler<ClientboundContainerClosePacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundContainerClosePacket packet, final ClientSession session) {
        CACHE.getPlayerCache().closeContainer(packet.getContainerId());
        syncPlayerEquipmentWithSpectatorsFromCache();
        return true;
    }
}
