package com.zenith.network.server.handler.shared.incoming;

import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundHelloPacket;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import lombok.NonNull;

public class HelloHandler implements PacketHandler<ServerboundHelloPacket, ServerConnection> {
    @Override
    public ServerboundHelloPacket apply(@NonNull ServerboundHelloPacket packet, @NonNull ServerConnection session) {
        return null;
    }
}
