package com.zenith.network.server.handler.shared.incoming;

import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.packet.common.serverbound.ServerboundKeepAlivePacket;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

public class KeepAliveHandler implements PacketHandler<ServerboundKeepAlivePacket, ServerConnection> {
    @Override
    public ServerboundKeepAlivePacket apply(final ServerboundKeepAlivePacket packet, final ServerConnection session) {
        if (packet.getPingId() == session.getLastPingId()) {
            long time = System.currentTimeMillis() - session.getLastPingTime();
            session.setFlag(MinecraftConstants.PING_KEY, time);
        }
        return null;
    }
}
