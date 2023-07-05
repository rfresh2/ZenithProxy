package com.zenith.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerWindowItemsPacket;
import com.zenith.feature.handler.HandlerRegistry;
import com.zenith.server.ServerConnection;

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
