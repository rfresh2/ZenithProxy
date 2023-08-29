package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetSlotPacket;
import com.zenith.network.registry.OutgoingHandler;
import com.zenith.network.server.ServerConnection;

public class ContainerSetSlotSpectatorOutgoingHandler implements OutgoingHandler<ClientboundContainerSetSlotPacket, ServerConnection> {
    @Override
    public ClientboundContainerSetSlotPacket apply(ClientboundContainerSetSlotPacket packet, ServerConnection session) {
        return null;
    }

    @Override
    public Class<ClientboundContainerSetSlotPacket> getPacketClass() {
        return ClientboundContainerSetSlotPacket.class;
    }
}
