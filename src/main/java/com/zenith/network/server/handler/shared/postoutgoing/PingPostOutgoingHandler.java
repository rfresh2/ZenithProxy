package com.zenith.network.server.handler.shared.postoutgoing;

import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundPingPacket;
import com.zenith.network.registry.PostOutgoingPacketHandler;
import com.zenith.network.server.ServerConnection;

public class PingPostOutgoingHandler implements PostOutgoingPacketHandler<ClientboundPingPacket, ServerConnection> {
    @Override
    public void accept(final ClientboundPingPacket packet, final ServerConnection session) {
        session.setLastPingId(packet.getId());
        session.setLastPingTime(System.currentTimeMillis());
    }
}
