package com.zenith.network.server.handler.shared.incoming;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.packet.status.clientbound.ClientboundPongResponsePacket;
import org.geysermc.mcprotocollib.protocol.packet.status.serverbound.ServerboundPingRequestPacket;

public class PingRequestHandler implements PacketHandler<ServerboundPingRequestPacket, ServerConnection> {
    public static final PingRequestHandler INSTANCE = new PingRequestHandler();
    @Override
    public ServerboundPingRequestPacket apply(final ServerboundPingRequestPacket packet, final ServerConnection session) {
        session.send(new ClientboundPongResponsePacket(packet.getPingTime()));
        return null;
    }
}
