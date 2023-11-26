package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetSlotPacket;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

public class ContainerSetSlotSpectatorOutgoingHandler implements PacketHandler<ClientboundContainerSetSlotPacket, ServerConnection> {
    @Override
    public ClientboundContainerSetSlotPacket apply(ClientboundContainerSetSlotPacket packet, ServerConnection session) {
        return null;
    }
}
