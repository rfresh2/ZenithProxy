package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundOpenBookPacket;
import com.zenith.network.registry.OutgoingHandler;
import com.zenith.network.server.ServerConnection;

public class OpenBookSpectatorOutgoingHandler implements OutgoingHandler<ClientboundOpenBookPacket, ServerConnection> {
    @Override
    public ClientboundOpenBookPacket apply(ClientboundOpenBookPacket packet, ServerConnection session) {
        return null;
    }

    @Override
    public Class<ClientboundOpenBookPacket> getPacketClass() {
        return ClientboundOpenBookPacket.class;
    }
}
