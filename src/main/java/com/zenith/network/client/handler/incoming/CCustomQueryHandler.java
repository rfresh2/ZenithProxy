package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundCustomQueryPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundCustomQueryAnswerPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;

public class CCustomQueryHandler implements PacketHandler<ClientboundCustomQueryPacket, ClientSession> {
    @Override
    public ClientboundCustomQueryPacket apply(final ClientboundCustomQueryPacket packet, final ClientSession session) {
        session.send(new ServerboundCustomQueryAnswerPacket(packet.getMessageId()));
        return null;
    }
}
