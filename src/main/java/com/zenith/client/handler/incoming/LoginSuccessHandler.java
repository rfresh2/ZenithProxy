package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.login.server.LoginSuccessPacket;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.util.Constants.CACHE;
public class LoginSuccessHandler implements HandlerRegistry.IncomingHandler<LoginSuccessPacket, ClientSession> {
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
