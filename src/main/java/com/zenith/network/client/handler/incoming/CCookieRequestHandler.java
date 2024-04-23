package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundCookieRequestPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;

public class CCookieRequestHandler implements PacketHandler<ClientboundCookieRequestPacket, ClientSession> {
    @Override
    public ClientboundCookieRequestPacket apply(final ClientboundCookieRequestPacket packet, final ClientSession session) {
        return null;
    }
}
