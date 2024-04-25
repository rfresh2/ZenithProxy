package com.zenith.network.server.handler.spectator.outgoing;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetDataPacket;

public class ContainerSetDataSpectatorOutgoingHandler implements PacketHandler<ClientboundContainerSetDataPacket, ServerConnection> {
    @Override
    public ClientboundContainerSetDataPacket apply(ClientboundContainerSetDataPacket packet, ServerConnection session) {
        return null;
    }
}
