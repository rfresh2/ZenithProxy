package com.zenith.server.handler.shared.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientKeepAlivePacket;
import com.zenith.feature.handler.HandlerRegistry;
import com.zenith.server.ServerConnection;
import lombok.NonNull;

public class ServerKeepaliveHandler implements HandlerRegistry.IncomingHandler<ClientKeepAlivePacket, ServerConnection> {
    @Override
    public boolean apply(@NonNull ClientKeepAlivePacket packet, @NonNull ServerConnection session) {
        final long serverSentPingTime = packet.getPingId();
        final long clientReceivedPingTime = System.nanoTime();
        session.setPing((clientReceivedPingTime - serverSentPingTime) / 1000000L);
        return false;
    }

    @Override
    public Class<ClientKeepAlivePacket> getPacketClass() {
        return ClientKeepAlivePacket.class;
    }
}
