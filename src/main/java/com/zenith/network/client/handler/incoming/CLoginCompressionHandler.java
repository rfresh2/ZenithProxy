package com.zenith.network.client.handler.incoming;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginCompressionPacket;

import static com.zenith.Shared.CONFIG;

public class CLoginCompressionHandler implements PacketHandler<ClientboundLoginCompressionPacket, ClientSession> {
    @Override
    public ClientboundLoginCompressionPacket apply(final ClientboundLoginCompressionPacket packet, final ClientSession session) {
        session.setCompressionThreshold(packet.getThreshold(), CONFIG.client.compressionLevel, false);
        return null;
    }
}
