package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundContainerClosePacket;
import com.zenith.network.registry.OutgoingHandler;
import com.zenith.network.server.ServerConnection;

public class ContainerCloseSpectatorOutgoingHandler implements OutgoingHandler<ClientboundContainerClosePacket, ServerConnection> {
    @Override
    public ClientboundContainerClosePacket apply(ClientboundContainerClosePacket packet, ServerConnection session) {
        return null;
    }
}
