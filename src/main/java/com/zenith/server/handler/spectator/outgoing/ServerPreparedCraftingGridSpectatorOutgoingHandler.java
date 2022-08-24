package com.zenith.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerPreparedCraftingGridPacket;
import com.zenith.server.PorkServerConnection;
import com.zenith.util.handler.HandlerRegistry;

public class ServerPreparedCraftingGridSpectatorOutgoingHandler implements HandlerRegistry.OutgoingHandler<ServerPreparedCraftingGridPacket, PorkServerConnection> {
    @Override
    public ServerPreparedCraftingGridPacket apply(ServerPreparedCraftingGridPacket packet, PorkServerConnection session) {
        return null;
    }

    @Override
    public Class<ServerPreparedCraftingGridPacket> getPacketClass() {
        return ServerPreparedCraftingGridPacket.class;
    }
}
