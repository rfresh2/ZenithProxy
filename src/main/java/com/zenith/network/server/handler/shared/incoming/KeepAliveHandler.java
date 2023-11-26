package com.zenith.network.server.handler.shared.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundKeepAlivePacket;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

public class KeepAliveHandler implements PacketHandler<ServerboundKeepAlivePacket, ServerConnection> {
    @Override
    public ServerboundKeepAlivePacket apply(final ServerboundKeepAlivePacket packet, final ServerConnection session) {
        return null;
    }
}
