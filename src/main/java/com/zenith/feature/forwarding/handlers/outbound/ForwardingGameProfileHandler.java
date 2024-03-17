package com.zenith.feature.forwarding.handlers.outbound;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundCustomQueryPacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundGameProfilePacket;
import com.zenith.module.impl.ProxyForwarding;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.Config;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE_MANAGER;

public class ForwardingGameProfileHandler implements PacketHandler<ClientboundGameProfilePacket, ServerConnection> {
    @Override
    public ClientboundGameProfilePacket apply(ClientboundGameProfilePacket packet, ServerConnection session) {
        final ProxyForwarding.ForwardedInfo forwardedInfo = MODULE_MANAGER.get(ProxyForwarding.class).popForwardedInfo(session);

        if (forwardedInfo == null || forwardedInfo.profile() == null) {
            if (forwardedInfo == null && CONFIG.server.extra.proxyForwarding.mode == Config.Server.Extra.ProxyForwarding.ForwardingMode.VELOCITY) {
                final byte[] data = new byte[]{ProxyForwarding.VELOCITY_MAX_SUPPORTED_FORWARDING_VERSION};
                session.sendAsync(new ClientboundCustomQueryPacket(ProxyForwarding.VELOCITY_QUERY_ID, ProxyForwarding.VELOCITY_PLAYER_INFO_CHANNEL.asString(), data));

                MODULE_MANAGER.get(ProxyForwarding.class).setForwardedInfo(session, new ProxyForwarding.ForwardedInfo(null, null));
            } else {
                session.disconnect("No proxy forwarding info received.");
            }
            return null;
        } else {
            GameProfile profile = forwardedInfo.profile();

            // BungeeCord doesn't forward profile name separately, so use existing one from the packet
            if (profile.getName() == null) {
                profile = new GameProfile(profile.getId(), packet.getProfile().getName());
                profile.setProperties(forwardedInfo.profile().getProperties());
            }

            session.setFlag(MinecraftConstants.PROFILE_KEY, profile);
            return packet.withProfile(profile);
        }
    }
}
