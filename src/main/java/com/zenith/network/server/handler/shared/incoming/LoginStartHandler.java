package com.zenith.network.server.handler.shared.incoming;

import com.github.steveice10.mc.protocol.packet.login.client.LoginStartPacket;
import com.zenith.network.registry.IncomingHandler;
import com.zenith.network.server.ServerConnection;
import lombok.NonNull;

public class LoginStartHandler implements IncomingHandler<LoginStartPacket, ServerConnection> {
    @Override
    public boolean apply(@NonNull LoginStartPacket packet, @NonNull ServerConnection session) {
        return false;
    }

    @Override
    public Class<LoginStartPacket> getPacketClass() {
        return LoginStartPacket.class;
    }
}
