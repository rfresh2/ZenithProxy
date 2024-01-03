package com.zenith.feature.forwarding.handlers.inbound;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.util.UUIDSerializer;
import com.github.steveice10.mc.protocol.data.handshake.HandshakeIntent;
import com.github.steveice10.mc.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import com.google.gson.reflect.TypeToken;
import com.zenith.module.impl.ProxyForwarding;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.Config;

import java.net.InetSocketAddress;
import java.util.List;

import static com.zenith.Shared.*;

public class ForwardingHandshakeHandler implements PacketHandler<ClientIntentionPacket, ServerConnection> {
    @Override
    public ClientIntentionPacket apply(ClientIntentionPacket packet, ServerConnection session) {
        if (packet.getIntent() == HandshakeIntent.LOGIN && CONFIG.client.extra.proxyForwarding.mode == Config.Client.Extra.ProxyForwarding.ForwardingMode.BUNGEECORD) {
            final String[] split = packet.getHostname().split("\00");
            if (split.length != 3 && split.length != 4) {
                session.disconnect("This server requires you to connect with BungeeCord.");
                return packet;
            }

            int remotePort = 0;
            if (session.getRemoteAddress() instanceof InetSocketAddress address) {
                remotePort = address.getPort();
            }
            final InetSocketAddress remoteAddress = new InetSocketAddress(split[1], remotePort);

            final GameProfile profile = new GameProfile(UUIDSerializer.fromString(split[2]), null);
            if (split.length == 4) {
                profile.setProperties(GSON.fromJson(split[3], new TypeToken<List<GameProfile.Property>>() {
                }.getType()));
            }

            session.setRemoteAddress(remoteAddress);
            MODULE_MANAGER.get(ProxyForwarding.class).setForwardedInfo(session, new ProxyForwarding.ForwardedInfo(profile, remoteAddress));
        }
        return packet;
    }
}
