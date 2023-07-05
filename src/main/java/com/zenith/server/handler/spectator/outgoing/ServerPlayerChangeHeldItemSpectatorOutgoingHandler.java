package com.zenith.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerChangeHeldItemPacket;
import com.zenith.feature.handler.HandlerRegistry;
import com.zenith.server.ServerConnection;

public class ServerPlayerChangeHeldItemSpectatorOutgoingHandler implements HandlerRegistry.OutgoingHandler<ServerPlayerChangeHeldItemPacket, ServerConnection> {
    @Override
    public ServerPlayerChangeHeldItemPacket apply(ServerPlayerChangeHeldItemPacket packet, ServerConnection session) {
        return null;
    }

    @Override
    public Class<ServerPlayerChangeHeldItemPacket> getPacketClass() {
        return ServerPlayerChangeHeldItemPacket.class;
    }
}
