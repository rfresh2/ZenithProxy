package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundStoreCookiePacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;

public class CStoreCookieHandler implements PacketHandler<ClientboundStoreCookiePacket, ClientSession> {
    @Override
    public ClientboundStoreCookiePacket apply(final ClientboundStoreCookiePacket packet, final ClientSession session) {
        // todo: store the cookie?
        return null;
    }
}
