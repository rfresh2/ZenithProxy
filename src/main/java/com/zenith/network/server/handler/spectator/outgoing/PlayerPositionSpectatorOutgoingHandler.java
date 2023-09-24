package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import com.zenith.network.registry.OutgoingHandler;
import com.zenith.network.server.ServerConnection;

public class PlayerPositionSpectatorOutgoingHandler implements OutgoingHandler<ClientboundPlayerPositionPacket, ServerConnection> {
    @Override
    public ClientboundPlayerPositionPacket apply(ClientboundPlayerPositionPacket packet, ServerConnection session) {
        if (session.isAllowSpectatorServerPlayerPosRotate()) {
            return packet;
        } else {
            return null;
        }
    }
}
