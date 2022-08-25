package com.zenith.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerSetSlotPacket;
import com.zenith.server.ServerConnection;
import com.zenith.util.handler.HandlerRegistry;

public class ServerSetSlotSpectatorOutgoingHandler implements HandlerRegistry.OutgoingHandler<ServerSetSlotPacket, ServerConnection> {
    @Override
    public ServerSetSlotPacket apply(ServerSetSlotPacket packet, ServerConnection session) {
        return null;
    }

    @Override
    public Class<ServerSetSlotPacket> getPacketClass() {
        return ServerSetSlotPacket.class;
    }
}
