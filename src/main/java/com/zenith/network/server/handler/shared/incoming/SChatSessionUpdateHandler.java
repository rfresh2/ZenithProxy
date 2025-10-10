package com.zenith.network.server.handler.shared.incoming;

import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatSessionUpdatePacket;

import static com.zenith.Globals.CONFIG;

public class SChatSessionUpdateHandler implements PacketHandler<ServerboundChatSessionUpdatePacket, ServerSession> {
    @Override
    public ServerboundChatSessionUpdatePacket apply(final ServerboundChatSessionUpdatePacket packet, final ServerSession session) {
        return switch (CONFIG.server.chatSigning.mode) {
            case PASSTHROUGH -> packet;
            case DISGUISED, SYSTEM -> null;
        };
    }
}
