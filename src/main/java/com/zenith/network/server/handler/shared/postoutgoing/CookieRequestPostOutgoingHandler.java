package com.zenith.network.server.handler.shared.postoutgoing;

import com.zenith.network.codec.PostOutgoingPacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundCookieRequestPacket;

public class CookieRequestPostOutgoingHandler implements PostOutgoingPacketHandler<ClientboundCookieRequestPacket, ServerSession> {
    @Override
    public void accept(final ClientboundCookieRequestPacket packet, final ServerSession session) {
        session.getCookieCache().getRequestedCookies().add(packet.getKey());
    }
}
