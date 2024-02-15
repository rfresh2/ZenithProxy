package com.zenith.network.server.handler.shared.incoming;

import com.github.steveice10.mc.protocol.packet.status.clientbound.ClientboundPongResponsePacket;
import com.github.steveice10.mc.protocol.packet.status.serverbound.ServerboundPingRequestPacket;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

public class PingRequestHandler implements PacketHandler<ServerboundPingRequestPacket, ServerConnection> {
    @Override
    public ServerboundPingRequestPacket apply(final ServerboundPingRequestPacket packet, final ServerConnection session) {
        session.send(new ClientboundPongResponsePacket(packet.getPingTime()));
        return null;
    }
}
