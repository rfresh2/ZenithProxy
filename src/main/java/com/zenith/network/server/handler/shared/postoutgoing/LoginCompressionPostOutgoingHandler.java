package com.zenith.network.server.handler.shared.postoutgoing;

import com.zenith.network.registry.PostOutgoingPacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginCompressionPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginFinishedPacket;

import static com.zenith.Shared.CONFIG;

public class LoginCompressionPostOutgoingHandler implements PostOutgoingPacketHandler<ClientboundLoginCompressionPacket, ServerSession> {
    @Override
    public void accept(final ClientboundLoginCompressionPacket packet, final ServerSession session) {
        session.setCompressionThreshold(packet.getThreshold(), CONFIG.server.compressionLevel, true);
        session.sendAsync(new ClientboundLoginFinishedPacket(session.getFlag(MinecraftConstants.PROFILE_KEY)));
    }
}
