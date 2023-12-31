package com.zenith.feature.forwarding.handlers.inbound;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.util.UUIDSerializer;
import com.github.steveice10.mc.protocol.data.handshake.HandshakeIntent;
import com.github.steveice10.mc.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import com.google.gson.reflect.TypeToken;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.Config;

import java.util.List;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.GSON;

public class ForwardingHandshakeHandler implements PacketHandler<ClientIntentionPacket, ServerConnection> {
    @Override
    public ClientIntentionPacket apply(ClientIntentionPacket packet, ServerConnection session) {
        if (packet.getIntent() == HandshakeIntent.LOGIN && CONFIG.client.extra.proxyForwarding.mode == Config.Client.Extra.ProxyForwarding.ForwardingMode.BUNGEECORD) {
            final String[] split = packet.getHostname().split("\00");
            if (split.length != 3 && split.length != 4) {
                session.disconnect("This server requires you to connect with BungeeCord.");
                return packet;
            }

            session.setSpoofedUuid(UUIDSerializer.fromString(split[2]));
            if (split.length == 4) {
                session.setSpoofedProperties(GSON.fromJson(split[3], new TypeToken<List<GameProfile.Property>>() {}.getType()));
            }
        }
        return packet;
    }
}
