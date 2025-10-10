package com.zenith.network.server.handler.shared.outgoing;

import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundDeleteChatPacket;

import static com.zenith.Globals.CONFIG;

public class SDeleteChatOutgoingHandler implements PacketHandler<ClientboundDeleteChatPacket, ServerSession> {
    @Override
    public ClientboundDeleteChatPacket apply(final ClientboundDeleteChatPacket packet, final ServerSession session) {
        return switch (CONFIG.server.chatSigning.mode) {
            case PASSTHROUGH -> packet;
            case DISGUISED, SYSTEM -> null;
        };
    }
}
