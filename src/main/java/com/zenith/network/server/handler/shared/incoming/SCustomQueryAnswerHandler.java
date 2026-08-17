package com.zenith.network.server.handler.shared.incoming;

import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundCustomQueryAnswerPacket;

import static com.zenith.Globals.CONFIG;

public class SCustomQueryAnswerHandler implements PacketHandler<ServerboundCustomQueryAnswerPacket, ServerSession> {
    @Override
    public ServerboundCustomQueryAnswerPacket apply(final ServerboundCustomQueryAnswerPacket packet, final ServerSession session) {
        if (CONFIG.server.strictLoginPacketSequence) {
            session.disconnect("Unexpected custom query answer packet");
        }
        return null;
    }
}
