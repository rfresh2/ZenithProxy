package com.zenith.network.server.handler.shared.incoming;

import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.status.clientbound.ClientboundPongResponsePacket;
import org.geysermc.mcprotocollib.protocol.packet.status.serverbound.ServerboundPingRequestPacket;

public class StatusPingRequestHandler implements PacketHandler<ServerboundPingRequestPacket, ServerSession> {
    public static final StatusPingRequestHandler INSTANCE = new StatusPingRequestHandler();
    @Override
    public ServerboundPingRequestPacket apply(final ServerboundPingRequestPacket packet, final ServerSession session) {
        session.send(new ClientboundPongResponsePacket(packet.getPingTime()));
        session.disconnect("Ping requested");
        return null;
    }
}
