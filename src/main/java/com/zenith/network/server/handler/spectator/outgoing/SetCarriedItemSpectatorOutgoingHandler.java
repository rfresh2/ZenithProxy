package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundSetCarriedItemPacket;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

public class SetCarriedItemSpectatorOutgoingHandler implements PacketHandler<ClientboundSetCarriedItemPacket, ServerConnection> {
    @Override
    public ClientboundSetCarriedItemPacket apply(ClientboundSetCarriedItemPacket packet, ServerConnection session) {
        return null;
    }
}
