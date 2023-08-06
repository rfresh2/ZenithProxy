package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerWindowPropertyPacket;
import com.zenith.network.registry.OutgoingHandler;
import com.zenith.network.server.ServerConnection;

public class ServerWindowPropertySpectatorOutgoingHandler implements OutgoingHandler<ServerWindowPropertyPacket, ServerConnection> {
    @Override
    public ServerWindowPropertyPacket apply(ServerWindowPropertyPacket packet, ServerConnection session) {
        return null;
    }

    @Override
    public Class<ServerWindowPropertyPacket> getPacketClass() {
        return ServerWindowPropertyPacket.class;
    }
}
