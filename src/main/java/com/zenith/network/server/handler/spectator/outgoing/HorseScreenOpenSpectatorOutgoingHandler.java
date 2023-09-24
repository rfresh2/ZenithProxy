package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundHorseScreenOpenPacket;
import com.zenith.network.registry.OutgoingHandler;
import com.zenith.network.server.ServerConnection;

public class HorseScreenOpenSpectatorOutgoingHandler implements OutgoingHandler<ClientboundHorseScreenOpenPacket, ServerConnection> {
    @Override
    public ClientboundHorseScreenOpenPacket apply(ClientboundHorseScreenOpenPacket packet, ServerConnection session) {
        return null;
    }
}
