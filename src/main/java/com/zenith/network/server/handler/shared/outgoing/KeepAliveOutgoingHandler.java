package com.zenith.network.server.handler.shared.outgoing;

import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundKeepAlivePacket;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

public class KeepAliveOutgoingHandler implements PacketHandler<ClientboundKeepAlivePacket, ServerConnection> {
    @Override
    public ClientboundKeepAlivePacket apply(final ClientboundKeepAlivePacket packet, final ServerConnection session) {
        session.setLastKeepAliveId(packet.getPingId());
        session.setLastKeepAliveTime(System.currentTimeMillis());
        return packet;
    }
}
