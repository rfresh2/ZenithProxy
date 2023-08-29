package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundGameProfilePacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.IncomingHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;
public class GameProfileHandler implements IncomingHandler<ClientboundGameProfilePacket, ClientSession> {
    @Override
    public boolean apply(@NonNull ClientboundGameProfilePacket packet, @NonNull ClientSession session) {
        CACHE.getProfileCache().setProfile(packet.getProfile());
        return false;
    }

    @Override
    public Class<ClientboundGameProfilePacket> getPacketClass() {
        return ClientboundGameProfilePacket.class;
    }
}
