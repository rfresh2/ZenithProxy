package com.zenith.feature.forwarding.handlers.outbound;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundGameProfilePacket;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

public class ForwardingGameProfileHandler implements PacketHandler<ClientboundGameProfilePacket, ServerConnection> {
    @Override
    public ClientboundGameProfilePacket apply(ClientboundGameProfilePacket packet, ServerConnection session) {
        if (session.getSpoofedUuid() != null) {
            final GameProfile profile = new GameProfile(session.getSpoofedUuid(), packet.getProfile().getName());
            profile.setProperties(session.getSpoofedProperties());

            session.setFlag(MinecraftConstants.PROFILE_KEY, profile);
            return packet.withProfile(profile);
        }
        return packet;
    }
}
