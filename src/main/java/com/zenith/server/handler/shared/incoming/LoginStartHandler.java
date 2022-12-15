package com.zenith.server.handler.shared.incoming;

import com.github.steveice10.mc.protocol.packet.login.client.LoginStartPacket;
import com.zenith.server.ServerConnection;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

public class LoginStartHandler implements HandlerRegistry.IncomingHandler<LoginStartPacket, ServerConnection> {
    @Override
    public boolean apply(@NonNull LoginStartPacket packet, @NonNull ServerConnection session) {
        return false;
    }

    @Override
    public Class<LoginStartPacket> getPacketClass() {
        return LoginStartPacket.class;
    }
}
