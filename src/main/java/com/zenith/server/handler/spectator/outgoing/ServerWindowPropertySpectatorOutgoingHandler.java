package com.zenith.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerWindowPropertyPacket;
import com.zenith.server.ServerConnection;
import com.zenith.util.handler.HandlerRegistry;

public class ServerWindowPropertySpectatorOutgoingHandler implements HandlerRegistry.OutgoingHandler<ServerWindowPropertyPacket, ServerConnection> {
    @Override
    public ServerWindowPropertyPacket apply(ServerWindowPropertyPacket packet, ServerConnection session) {
        return null;
    }

    @Override
    public Class<ServerWindowPropertyPacket> getPacketClass() {
        return ServerWindowPropertyPacket.class;
    }
}
