package com.zenith.network.client.handler.incoming;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginDisconnectPacket;

public class LoginDisconnectHandler implements PacketHandler<ClientboundLoginDisconnectPacket, ClientSession> {
    @Override
    public ClientboundLoginDisconnectPacket apply(final ClientboundLoginDisconnectPacket packet, final ClientSession session) {
        session.disconnect(packet.getReason());
        return null;
    }
}
