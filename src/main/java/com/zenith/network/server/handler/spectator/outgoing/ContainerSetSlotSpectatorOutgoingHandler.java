package com.zenith.network.server.handler.spectator.outgoing;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetSlotPacket;

public class ContainerSetSlotSpectatorOutgoingHandler implements PacketHandler<ClientboundContainerSetSlotPacket, ServerConnection> {
    @Override
    public ClientboundContainerSetSlotPacket apply(ClientboundContainerSetSlotPacket packet, ServerConnection session) {
        return null;
    }
}
