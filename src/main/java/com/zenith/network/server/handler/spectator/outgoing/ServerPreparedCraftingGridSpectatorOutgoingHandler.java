package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerPreparedCraftingGridPacket;
import com.zenith.network.registry.OutgoingHandler;
import com.zenith.network.server.ServerConnection;

public class ServerPreparedCraftingGridSpectatorOutgoingHandler implements OutgoingHandler<ServerPreparedCraftingGridPacket, ServerConnection> {
    @Override
    public ServerPreparedCraftingGridPacket apply(ServerPreparedCraftingGridPacket packet, ServerConnection session) {
        return null;
    }

    @Override
    public Class<ServerPreparedCraftingGridPacket> getPacketClass() {
        return ServerPreparedCraftingGridPacket.class;
    }
}
