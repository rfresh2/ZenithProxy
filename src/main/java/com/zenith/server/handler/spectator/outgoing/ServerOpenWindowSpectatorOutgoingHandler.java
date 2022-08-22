package com.zenith.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerOpenWindowPacket;
import com.zenith.server.PorkServerConnection;
import com.zenith.util.handler.HandlerRegistry;

public class ServerOpenWindowSpectatorOutgoingHandler implements HandlerRegistry.OutgoingHandler<ServerOpenWindowPacket, PorkServerConnection> {
    @Override
    public ServerOpenWindowPacket apply(ServerOpenWindowPacket packet, PorkServerConnection session) {
        return null;
    }

    @Override
    public Class<ServerOpenWindowPacket> getPacketClass() {
        return ServerOpenWindowPacket.class;
    }
}
