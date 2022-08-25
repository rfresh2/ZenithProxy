package com.zenith.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerHealthPacket;
import com.zenith.server.ServerConnection;
import com.zenith.util.handler.HandlerRegistry;

public class ServerPlayerHealthSpectatorOutgoingHandler implements HandlerRegistry.OutgoingHandler<ServerPlayerHealthPacket, ServerConnection> {
    @Override
    public ServerPlayerHealthPacket apply(ServerPlayerHealthPacket packet, ServerConnection session) {
        return null;
    }

    @Override
    public Class<ServerPlayerHealthPacket> getPacketClass() {
        return ServerPlayerHealthPacket.class;
    }
}
