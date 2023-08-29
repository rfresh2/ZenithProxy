package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundSetCarriedItemPacket;
import com.zenith.network.registry.OutgoingHandler;
import com.zenith.network.server.ServerConnection;

public class ClientboundSetCarriedItemSpectatorOutgoingHandler implements OutgoingHandler<ClientboundSetCarriedItemPacket, ServerConnection> {
    @Override
    public ClientboundSetCarriedItemPacket apply(ClientboundSetCarriedItemPacket packet, ServerConnection session) {
        return null;
    }

    @Override
    public Class<ClientboundSetCarriedItemPacket> getPacketClass() {
        return ClientboundSetCarriedItemPacket.class;
    }
}
