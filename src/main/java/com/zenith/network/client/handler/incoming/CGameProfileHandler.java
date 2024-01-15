package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundGameProfilePacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundLoginAcknowledgedPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;

import static com.zenith.Shared.CACHE;

public class CGameProfileHandler implements PacketHandler<ClientboundGameProfilePacket, ClientSession> {
    @Override
    public ClientboundGameProfilePacket apply(final ClientboundGameProfilePacket packet, final ClientSession session) {
        CACHE.getProfileCache().setProfile(packet.getProfile());
        session.send(new ServerboundLoginAcknowledgedPacket());
        return null;
    }
}
