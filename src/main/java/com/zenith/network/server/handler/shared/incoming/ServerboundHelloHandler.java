package com.zenith.network.server.handler.shared.incoming;

import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundHelloPacket;
import com.zenith.network.registry.IncomingHandler;
import com.zenith.network.server.ServerConnection;
import lombok.NonNull;

public class ServerboundHelloHandler implements IncomingHandler<ServerboundHelloPacket, ServerConnection> {
    @Override
    public boolean apply(@NonNull ServerboundHelloPacket packet, @NonNull ServerConnection session) {
        return false;
    }

    @Override
    public Class<ServerboundHelloPacket> getPacketClass() {
        return ServerboundHelloPacket.class;
    }
}
