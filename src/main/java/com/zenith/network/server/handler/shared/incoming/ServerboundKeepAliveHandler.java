package com.zenith.network.server.handler.shared.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundKeepAlivePacket;
import com.zenith.network.registry.IncomingHandler;
import com.zenith.network.server.ServerConnection;

public class ServerboundKeepAliveHandler implements IncomingHandler<ServerboundKeepAlivePacket, ServerConnection> {
    @Override
    public boolean apply(final ServerboundKeepAlivePacket packet, final ServerConnection session) {
        return false;
    }

    @Override
    public Class<ServerboundKeepAlivePacket> getPacketClass() {
        return ServerboundKeepAlivePacket.class;
    }
}
