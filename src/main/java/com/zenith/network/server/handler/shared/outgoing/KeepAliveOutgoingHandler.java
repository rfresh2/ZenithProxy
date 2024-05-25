package com.zenith.network.server.handler.shared.outgoing;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundKeepAlivePacket;

public class KeepAliveOutgoingHandler implements PacketHandler<ClientboundKeepAlivePacket, ServerConnection> {
    public static final KeepAliveOutgoingHandler INSTANCE = new KeepAliveOutgoingHandler();
    @Override
    public ClientboundKeepAlivePacket apply(final ClientboundKeepAlivePacket packet, final ServerConnection session) {
        session.setLastKeepAliveId(packet.getPingId());
        session.setLastKeepAliveTime(System.currentTimeMillis());
        return packet;
    }
}
