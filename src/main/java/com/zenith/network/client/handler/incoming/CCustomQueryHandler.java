package com.zenith.network.client.handler.incoming;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundCustomQueryPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundCustomQueryAnswerPacket;

public class CCustomQueryHandler implements PacketHandler<ClientboundCustomQueryPacket, ClientSession> {
    @Override
    public ClientboundCustomQueryPacket apply(final ClientboundCustomQueryPacket packet, final ClientSession session) {
        session.send(new ServerboundCustomQueryAnswerPacket(packet.getMessageId()));
        return null;
    }
}
