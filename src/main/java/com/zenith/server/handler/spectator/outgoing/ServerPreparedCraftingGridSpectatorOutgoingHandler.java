package com.zenith.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerPreparedCraftingGridPacket;
import com.zenith.feature.handler.HandlerRegistry;
import com.zenith.server.ServerConnection;

public class ServerPreparedCraftingGridSpectatorOutgoingHandler implements HandlerRegistry.OutgoingHandler<ServerPreparedCraftingGridPacket, ServerConnection> {
    @Override
    public ServerPreparedCraftingGridPacket apply(ServerPreparedCraftingGridPacket packet, ServerConnection session) {
        return null;
    }

    @Override
    public Class<ServerPreparedCraftingGridPacket> getPacketClass() {
        return ServerPreparedCraftingGridPacket.class;
    }
}
