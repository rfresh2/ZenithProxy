package com.zenith.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.zenith.server.ServerConnection;
import com.zenith.util.handler.HandlerRegistry;

public class ServerPlayerPositionRotationSpectatorOutgoingHandler implements HandlerRegistry.OutgoingHandler<ServerPlayerPositionRotationPacket, ServerConnection> {
    @Override
    public ServerPlayerPositionRotationPacket apply(ServerPlayerPositionRotationPacket packet, ServerConnection session) {
        if (session.isAllowSpectatorServerPlayerPosRotate()) {
            return packet;
        } else {
            return null;
        }
    }

    @Override
    public Class<ServerPlayerPositionRotationPacket> getPacketClass() {
        return ServerPlayerPositionRotationPacket.class;
    }
}
