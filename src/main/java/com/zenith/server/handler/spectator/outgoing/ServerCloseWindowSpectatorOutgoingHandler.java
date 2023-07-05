package com.zenith.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerCloseWindowPacket;
import com.zenith.feature.handler.HandlerRegistry;
import com.zenith.server.ServerConnection;

public class ServerCloseWindowSpectatorOutgoingHandler implements HandlerRegistry.OutgoingHandler<ServerCloseWindowPacket, ServerConnection> {
    @Override
    public ServerCloseWindowPacket apply(ServerCloseWindowPacket packet, ServerConnection session) {
        return null;
    }

    @Override
    public Class<ServerCloseWindowPacket> getPacketClass() {
        return ServerCloseWindowPacket.class;
    }
}
