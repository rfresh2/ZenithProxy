package com.zenith.network.server.handler.spectator.incoming;

import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundKeepAlivePacket;

public class KeepAliveSpectatorHandler implements PacketHandler<ServerboundKeepAlivePacket, ServerSession> {
    public static final KeepAliveSpectatorHandler INSTANCE = new KeepAliveSpectatorHandler();
    @Override
    public ServerboundKeepAlivePacket apply(final ServerboundKeepAlivePacket packet, final ServerSession session) {
        if (packet.getPingId() == session.getLastKeepAliveId()) {
            session.setPing(System.currentTimeMillis() - session.getLastKeepAliveTime());
        }
        return null;
    }
}
