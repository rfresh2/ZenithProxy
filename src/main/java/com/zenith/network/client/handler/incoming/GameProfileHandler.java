package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundGameProfilePacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;
public class GameProfileHandler implements PacketHandler<ClientboundGameProfilePacket, ClientSession> {
    @Override
    public ClientboundGameProfilePacket apply(@NonNull ClientboundGameProfilePacket packet, @NonNull ClientSession session) {
        CACHE.getProfileCache().setProfile(packet.getProfile());
        return null;
    }
}
