package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerKeepAlivePacket;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

public class ClientKeepaliveHandler implements HandlerRegistry.IncomingHandler<ServerKeepAlivePacket, ClientSession> {
    @Override
    public boolean apply(@NonNull ServerKeepAlivePacket packet, @NonNull ClientSession session) {
        return false;
    }

    @Override
    public Class<ServerKeepAlivePacket> getPacketClass() {
        return ServerKeepAlivePacket.class;
    }
}
