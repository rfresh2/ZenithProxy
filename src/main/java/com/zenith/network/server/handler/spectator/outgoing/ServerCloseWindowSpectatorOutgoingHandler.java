package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerCloseWindowPacket;
import com.zenith.network.registry.OutgoingHandler;
import com.zenith.network.server.ServerConnection;

public class ServerCloseWindowSpectatorOutgoingHandler implements OutgoingHandler<ServerCloseWindowPacket, ServerConnection> {
    @Override
    public ServerCloseWindowPacket apply(ServerCloseWindowPacket packet, ServerConnection session) {
        return null;
    }

    @Override
    public Class<ServerCloseWindowPacket> getPacketClass() {
        return ServerCloseWindowPacket.class;
    }
}
