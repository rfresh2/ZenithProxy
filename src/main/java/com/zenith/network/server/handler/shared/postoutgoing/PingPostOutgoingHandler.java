package com.zenith.network.server.handler.shared.postoutgoing;

import com.zenith.network.registry.PostOutgoingPacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundPingPacket;

public class PingPostOutgoingHandler implements PostOutgoingPacketHandler<ClientboundPingPacket, ServerConnection> {
    @Override
    public void accept(final ClientboundPingPacket packet, final ServerConnection session) {
        session.setLastPingId(packet.getId());
        session.setLastPingTime(System.currentTimeMillis());
    }
}
