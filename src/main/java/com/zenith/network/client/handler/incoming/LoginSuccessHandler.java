package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.login.server.LoginSuccessPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.IncomingHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;
public class LoginSuccessHandler implements IncomingHandler<LoginSuccessPacket, ClientSession> {
    @Override
    public boolean apply(@NonNull LoginSuccessPacket packet, @NonNull ClientSession session) {
        CACHE.getProfileCache().setProfile(packet.getProfile());
        return false;
    }

    @Override
    public Class<LoginSuccessPacket> getPacketClass() {
        return LoginSuccessPacket.class;
    }
}
