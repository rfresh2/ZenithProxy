package com.zenith.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerWindowItemsPacket;
import com.zenith.server.ServerConnection;
import com.zenith.util.handler.HandlerRegistry;

public class ServerWindowItemsSpectatorOutgoingHandler implements HandlerRegistry.OutgoingHandler<ServerWindowItemsPacket, ServerConnection> {
    @Override
    public ServerWindowItemsPacket apply(ServerWindowItemsPacket packet, ServerConnection session) {
        return null;
    }

    @Override
    public Class<ServerWindowItemsPacket> getPacketClass() {
        return ServerWindowItemsPacket.class;
    }
}
