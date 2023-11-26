package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetContentPacket;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

public class ContainerSetContentSpectatorOutgoingHandler implements PacketHandler<ClientboundContainerSetContentPacket, ServerConnection> {
    @Override
    public ClientboundContainerSetContentPacket apply(ClientboundContainerSetContentPacket packet, ServerConnection session) {
        return null;
    }
}
