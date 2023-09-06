package com.zenith.network.server.handler.shared.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPingPacket;
import com.zenith.network.registry.OutgoingHandler;
import com.zenith.network.server.ServerConnection;

public class PingOutgoingHandler implements OutgoingHandler<ClientboundPingPacket, ServerConnection> {
    @Override
    public ClientboundPingPacket apply(final ClientboundPingPacket packet, final ServerConnection session) {
        session.setLastPingId(packet.getId());
        session.setLastPingTime(System.currentTimeMillis());
        return packet;
    }

    @Override
    public Class<ClientboundPingPacket> getPacketClass() {
        return null;
    }
}
