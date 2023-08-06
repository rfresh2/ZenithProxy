package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerSetSlotPacket;
import com.zenith.network.registry.OutgoingHandler;
import com.zenith.network.server.ServerConnection;

public class ServerSetSlotSpectatorOutgoingHandler implements OutgoingHandler<ServerSetSlotPacket, ServerConnection> {
    @Override
    public ServerSetSlotPacket apply(ServerSetSlotPacket packet, ServerConnection session) {
        return null;
    }

    @Override
    public Class<ServerSetSlotPacket> getPacketClass() {
        return ServerSetSlotPacket.class;
    }
}
