package com.zenith.network.client.handler.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.OutgoingHandler;

public class OutgoingChatHandler implements OutgoingHandler<ServerboundChatPacket, ClientSession> {
    @Override
    public ServerboundChatPacket apply(final ServerboundChatPacket packet, final ClientSession session) {
        // allow us to dispatch commands just with chat packets
        if (packet.getMessage().startsWith("/")) {
            session.send(new ServerboundChatCommandPacket(packet.getMessage().substring(1)));
            return null;
        }
        return packet;
    }
}
