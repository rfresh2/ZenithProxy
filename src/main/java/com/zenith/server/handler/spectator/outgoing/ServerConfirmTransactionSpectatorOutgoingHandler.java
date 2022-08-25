package com.zenith.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerConfirmTransactionPacket;
import com.zenith.server.ServerConnection;
import com.zenith.util.handler.HandlerRegistry;

public class ServerConfirmTransactionSpectatorOutgoingHandler implements HandlerRegistry.OutgoingHandler<ServerConfirmTransactionPacket, ServerConnection> {
    @Override
    public ServerConfirmTransactionPacket apply(ServerConfirmTransactionPacket packet, ServerConnection session) {
        return null;
    }

    @Override
    public Class<ServerConfirmTransactionPacket> getPacketClass() {
        return ServerConfirmTransactionPacket.class;
    }
}
