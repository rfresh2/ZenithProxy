package com.zenith.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerOpenWindowPacket;
import com.zenith.feature.handler.HandlerRegistry;
import com.zenith.server.ServerConnection;

public class ServerOpenWindowSpectatorOutgoingHandler implements HandlerRegistry.OutgoingHandler<ServerOpenWindowPacket, ServerConnection> {
    @Override
    public ServerOpenWindowPacket apply(ServerOpenWindowPacket packet, ServerConnection session) {
        return null;
    }

    @Override
    public Class<ServerOpenWindowPacket> getPacketClass() {
        return ServerOpenWindowPacket.class;
    }
}
