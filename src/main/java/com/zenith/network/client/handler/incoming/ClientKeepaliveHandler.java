package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerKeepAlivePacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.IncomingHandler;
import lombok.NonNull;

public class ClientKeepaliveHandler implements IncomingHandler<ServerKeepAlivePacket, ClientSession> {
    @Override
    public boolean apply(@NonNull ServerKeepAlivePacket packet, @NonNull ClientSession session) {
        return false;
    }

    @Override
    public Class<ServerKeepAlivePacket> getPacketClass() {
        return ServerKeepAlivePacket.class;
    }
}
