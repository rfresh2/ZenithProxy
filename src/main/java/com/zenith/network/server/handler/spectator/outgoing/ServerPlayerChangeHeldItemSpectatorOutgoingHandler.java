package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerChangeHeldItemPacket;
import com.zenith.network.registry.OutgoingHandler;
import com.zenith.network.server.ServerConnection;

public class ServerPlayerChangeHeldItemSpectatorOutgoingHandler implements OutgoingHandler<ServerPlayerChangeHeldItemPacket, ServerConnection> {
    @Override
    public ServerPlayerChangeHeldItemPacket apply(ServerPlayerChangeHeldItemPacket packet, ServerConnection session) {
        return null;
    }

    @Override
    public Class<ServerPlayerChangeHeldItemPacket> getPacketClass() {
        return ServerPlayerChangeHeldItemPacket.class;
    }
}
