package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerWindowItemsPacket;
import com.zenith.network.registry.OutgoingHandler;
import com.zenith.network.server.ServerConnection;

public class ServerWindowItemsSpectatorOutgoingHandler implements OutgoingHandler<ServerWindowItemsPacket, ServerConnection> {
    @Override
    public ServerWindowItemsPacket apply(ServerWindowItemsPacket packet, ServerConnection session) {
        return null;
    }

    @Override
    public Class<ServerWindowItemsPacket> getPacketClass() {
        return ServerWindowItemsPacket.class;
    }
}
