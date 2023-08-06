package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerOpenWindowPacket;
import com.zenith.network.registry.OutgoingHandler;
import com.zenith.network.server.ServerConnection;

public class ServerOpenWindowSpectatorOutgoingHandler implements OutgoingHandler<ServerOpenWindowPacket, ServerConnection> {
    @Override
    public ServerOpenWindowPacket apply(ServerOpenWindowPacket packet, ServerConnection session) {
        return null;
    }

    @Override
    public Class<ServerOpenWindowPacket> getPacketClass() {
        return ServerOpenWindowPacket.class;
    }
}
