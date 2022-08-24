package com.zenith.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.zenith.server.PorkServerConnection;
import com.zenith.util.handler.HandlerRegistry;

public class ServerPlayerPositionRotationSpectatorOutgoingHandler implements HandlerRegistry.OutgoingHandler<ServerPlayerPositionRotationPacket, PorkServerConnection> {
    @Override
    public ServerPlayerPositionRotationPacket apply(ServerPlayerPositionRotationPacket packet, PorkServerConnection session) {
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
