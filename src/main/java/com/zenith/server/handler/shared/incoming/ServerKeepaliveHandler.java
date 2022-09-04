package com.zenith.server.handler.shared.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientKeepAlivePacket;
import com.zenith.server.ServerConnection;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

public class ServerKeepaliveHandler implements HandlerRegistry.IncomingHandler<ClientKeepAlivePacket, ServerConnection> {
    @Override
    public boolean apply(@NonNull ClientKeepAlivePacket packet, @NonNull ServerConnection session) {
        return false;
    }

    @Override
    public Class<ClientKeepAlivePacket> getPacketClass() {
        return ClientKeepAlivePacket.class;
    }
}
