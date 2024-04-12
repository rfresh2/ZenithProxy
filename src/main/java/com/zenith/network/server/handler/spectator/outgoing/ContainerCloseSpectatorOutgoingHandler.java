package com.zenith.network.server.handler.spectator.outgoing;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerClosePacket;

public class ContainerCloseSpectatorOutgoingHandler implements PacketHandler<ClientboundContainerClosePacket, ServerConnection> {
    @Override
    public ClientboundContainerClosePacket apply(ClientboundContainerClosePacket packet, ServerConnection session) {
        return null;
    }
}
