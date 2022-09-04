package com.zenith.server.handler.shared.outgoing;

import com.github.steveice10.mc.protocol.packet.login.server.LoginSuccessPacket;
import com.zenith.server.ServerConnection;
import com.zenith.util.Wait;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.util.Constants.*;
import static java.util.Objects.isNull;

public class LoginSuccessOutgoingHandler implements HandlerRegistry.OutgoingHandler<LoginSuccessPacket, ServerConnection> {
    @Override
    public LoginSuccessPacket apply(@NonNull LoginSuccessPacket packet, @NonNull ServerConnection session) {
        // profile could be null at this point?
        int tryCount = 0;
        while (tryCount < 3 && CACHE.getProfileCache().getProfile() == null) {
            Wait.waitALittleMs(500);
            tryCount++;
        }
        if (CACHE.getProfileCache().getProfile() == null) {
            session.disconnect(MANUAL_DISCONNECT);
            return null;
        } else {
            SERVER_LOG.debug("User UUID: %s\nBot UUID: %s", packet.getProfile().getId().toString(), CACHE.getProfileCache().getProfile().getId().toString());
            session.getProfileCache().setProfile(packet.getProfile());
            if (isNull(session.getProxy().getCurrentPlayer().get()))
            {
                return new LoginSuccessPacket(CACHE.getProfileCache().getProfile());
            } else {
                return new LoginSuccessPacket(session.getProfileCache().getProfile());
            }
        }
    }

    @Override
    public Class<LoginSuccessPacket> getPacketClass() {
        return LoginSuccessPacket.class;
    }
}
