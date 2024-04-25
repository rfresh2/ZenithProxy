package com.zenith.network.server.handler.spectator.outgoing;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetContentPacket;

public class ContainerSetContentSpectatorOutgoingHandler implements PacketHandler<ClientboundContainerSetContentPacket, ServerConnection> {
    @Override
    public ClientboundContainerSetContentPacket apply(ClientboundContainerSetContentPacket packet, ServerConnection session) {
        return null;
    }
}
