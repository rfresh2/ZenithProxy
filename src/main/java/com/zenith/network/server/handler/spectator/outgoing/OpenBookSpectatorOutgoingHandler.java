package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundOpenBookPacket;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

public class OpenBookSpectatorOutgoingHandler implements PacketHandler<ClientboundOpenBookPacket, ServerConnection> {
    @Override
    public ClientboundOpenBookPacket apply(ClientboundOpenBookPacket packet, ServerConnection session) {
        return null;
    }
}
