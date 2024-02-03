package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundLoginCompressionPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;

import static com.zenith.Shared.CONFIG;

public class CLoginCompressionHandler implements PacketHandler<ClientboundLoginCompressionPacket, ClientSession> {
    @Override
    public ClientboundLoginCompressionPacket apply(final ClientboundLoginCompressionPacket packet, final ClientSession session) {
        session.setCompressionThreshold(packet.getThreshold(), CONFIG.client.compressionLevel, false);
        return null;
    }
}
