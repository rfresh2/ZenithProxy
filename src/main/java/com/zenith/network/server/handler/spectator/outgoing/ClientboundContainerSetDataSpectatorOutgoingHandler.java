package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetDataPacket;
import com.zenith.network.registry.OutgoingHandler;
import com.zenith.network.server.ServerConnection;

public class ClientboundContainerSetDataSpectatorOutgoingHandler implements OutgoingHandler<ClientboundContainerSetDataPacket, ServerConnection> {
    @Override
    public ClientboundContainerSetDataPacket apply(ClientboundContainerSetDataPacket packet, ServerConnection session) {
        return null;
    }

    @Override
    public Class<ClientboundContainerSetDataPacket> getPacketClass() {
        return ClientboundContainerSetDataPacket.class;
    }
}
