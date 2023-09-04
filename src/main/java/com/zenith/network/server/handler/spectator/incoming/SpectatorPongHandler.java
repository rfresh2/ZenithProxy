package com.zenith.network.server.handler.spectator.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundPongPacket;
import com.zenith.network.registry.IncomingHandler;
import com.zenith.network.server.ServerConnection;
import lombok.NonNull;

public class SpectatorPongHandler implements IncomingHandler<ServerboundPongPacket, ServerConnection> {
    @Override
    public boolean apply(@NonNull ServerboundPongPacket packet, @NonNull ServerConnection session) {
        final long serverSentPingTime = packet.getId();
        final long clientReceivedPingTime = System.nanoTime();
        session.setPing((clientReceivedPingTime - serverSentPingTime) / 1000000L);
        return false;
    }

    @Override
    public Class<ServerboundPongPacket> getPacketClass() {
        return ServerboundPongPacket.class;
    }
}
