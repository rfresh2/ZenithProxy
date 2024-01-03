package com.zenith.feature.forwarding.handlers.inbound;

import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundCustomQueryPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundHelloPacket;
import com.zenith.module.impl.ProxyForwarding;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.Config;

import static com.zenith.Shared.CONFIG;

public class ForwardingHelloHandler implements PacketHandler<ServerboundHelloPacket, ServerConnection> {
    @Override
    public ServerboundHelloPacket apply(ServerboundHelloPacket packet, ServerConnection session) {
        if (CONFIG.client.extra.proxyForwarding.mode == Config.Client.Extra.ProxyForwarding.ForwardingMode.VELOCITY) {
            final byte[] data = new byte[]{ProxyForwarding.VELOCITY_MAX_SUPPORTED_FORWARDING_VERSION};
            session.sendAsync(new ClientboundCustomQueryPacket(ProxyForwarding.VELOCITY_QUERY_ID, ProxyForwarding.VELOCITY_PLAYER_INFO_CHANNEL.asString(), data));
        }
        return packet;
    }
}
