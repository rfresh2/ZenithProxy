package com.zenith.network.server.handler.player.incoming;

import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundKeepAlivePacket;

import static com.zenith.Globals.CONFIG;

public class SPlayerKeepAliveHandler implements PacketHandler<ServerboundKeepAlivePacket, ServerSession> {
    public static final SPlayerKeepAliveHandler INSTANCE = new SPlayerKeepAliveHandler();
    @Override
    public ServerboundKeepAlivePacket apply(final ServerboundKeepAlivePacket packet, final ServerSession session) {
        if (packet.getPingId() == session.getLastKeepAliveId()) {
            session.setPing(System.currentTimeMillis() - session.getLastKeepAliveTime());
        }
        return switch (CONFIG.client.keepAliveHandling.keepAliveMode) {
            case INDEPENDENT -> null;
            case PASSTHROUGH -> packet;
        };
    }
}
