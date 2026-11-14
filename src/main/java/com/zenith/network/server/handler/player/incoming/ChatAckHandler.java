package com.zenith.network.server.handler.player.incoming;

import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatAckPacket;

public class ChatAckHandler implements PacketHandler<ServerboundChatAckPacket, ServerSession> {
    @Override
    public ServerboundChatAckPacket apply(final ServerboundChatAckPacket packet, final ServerSession session) {
        return null;
    }
}
