package com.zenith.network.server.handler.shared.outgoing;

import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;

public class SRespawnOutgoingHandler implements PacketHandler<ClientboundRespawnPacket, ServerSession> {
    @Override
    public ClientboundRespawnPacket apply(final ClientboundRespawnPacket packet, final ServerSession session) {
        session.setClientLoaded(false);
        return packet;
    }
}
