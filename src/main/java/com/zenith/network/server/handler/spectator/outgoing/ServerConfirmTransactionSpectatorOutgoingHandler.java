package com.zenith.network.server.handler.spectator.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerConfirmTransactionPacket;
import com.zenith.network.registry.OutgoingHandler;
import com.zenith.network.server.ServerConnection;

public class ServerConfirmTransactionSpectatorOutgoingHandler implements OutgoingHandler<ServerConfirmTransactionPacket, ServerConnection> {
    @Override
    public ServerConfirmTransactionPacket apply(ServerConfirmTransactionPacket packet, ServerConnection session) {
        return null;
    }

    @Override
    public Class<ServerConfirmTransactionPacket> getPacketClass() {
        return ServerConfirmTransactionPacket.class;
    }
}
