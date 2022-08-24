package com.zenith.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerChangeHeldItemPacket;
import com.zenith.server.PorkServerConnection;
import com.zenith.util.handler.HandlerRegistry;

public class ServerPlayerChangeHeldItemSpectatorOutgoingHandler implements HandlerRegistry.OutgoingHandler<ServerPlayerChangeHeldItemPacket, PorkServerConnection> {
    @Override
    public ServerPlayerChangeHeldItemPacket apply(ServerPlayerChangeHeldItemPacket packet, PorkServerConnection session) {
        return null;
    }

    @Override
    public Class<ServerPlayerChangeHeldItemPacket> getPacketClass() {
        return ServerPlayerChangeHeldItemPacket.class;
    }
}
