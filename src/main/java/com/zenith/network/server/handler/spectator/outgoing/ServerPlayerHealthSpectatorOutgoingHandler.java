package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerHealthPacket;
import com.zenith.network.registry.OutgoingHandler;
import com.zenith.network.server.ServerConnection;

public class ServerPlayerHealthSpectatorOutgoingHandler implements OutgoingHandler<ServerPlayerHealthPacket, ServerConnection> {
    @Override
    public ServerPlayerHealthPacket apply(ServerPlayerHealthPacket packet, ServerConnection session) {
        return null;
    }

    @Override
    public Class<ServerPlayerHealthPacket> getPacketClass() {
        return ServerPlayerHealthPacket.class;
    }
}
