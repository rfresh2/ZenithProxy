package com.zenith.network.client.handler.incoming;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundDisconnectPacket;

public class CDisconnectHandler implements PacketHandler<ClientboundDisconnectPacket, ClientSession> {
    @Override
    public ClientboundDisconnectPacket apply(final ClientboundDisconnectPacket packet, final ClientSession session) {
        session.disconnect(packet.getReason());
        return null;
    }
}
