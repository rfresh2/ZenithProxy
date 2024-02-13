package com.zenith.network.client.handler.postoutgoing;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClosePacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PostOutgoingPacketHandler;

import static com.zenith.Shared.CACHE;

public class PostOutgoingContainerCloseHandler implements PostOutgoingPacketHandler<ServerboundContainerClosePacket, ClientSession> {
    @Override
    public void accept(final ServerboundContainerClosePacket packet, final ClientSession session) {
        CACHE.getPlayerCache().closeContainer(packet.getContainerId());
    }
}
