package com.zenith.network.server.handler.shared.incoming;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundKeepAlivePacket;

public class KeepAliveHandler implements PacketHandler<ServerboundKeepAlivePacket, ServerConnection> {
    public static final KeepAliveHandler INSTANCE = new KeepAliveHandler();
    @Override
    public ServerboundKeepAlivePacket apply(final ServerboundKeepAlivePacket packet, final ServerConnection session) {
        if (packet.getPingId() == session.getLastKeepAliveId()) {
            session.setPing(System.currentTimeMillis() - session.getLastKeepAliveTime());
        }
        return null;
    }
}
