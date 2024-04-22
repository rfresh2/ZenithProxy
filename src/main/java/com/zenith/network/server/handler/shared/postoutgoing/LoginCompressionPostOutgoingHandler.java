package com.zenith.network.server.handler.shared.postoutgoing;

import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundGameProfilePacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundLoginCompressionPacket;
import com.zenith.network.registry.PostOutgoingPacketHandler;
import com.zenith.network.server.ServerConnection;

import static com.zenith.Shared.CONFIG;

public class LoginCompressionPostOutgoingHandler implements PostOutgoingPacketHandler<ClientboundLoginCompressionPacket, ServerConnection> {
    @Override
    public void accept(final ClientboundLoginCompressionPacket packet, final ServerConnection session) {
        session.setCompressionThreshold(packet.getThreshold(), CONFIG.server.compressionLevel, true);
        session.send(new ClientboundGameProfilePacket(session.getFlag(MinecraftConstants.PROFILE_KEY), false));
    }
}
