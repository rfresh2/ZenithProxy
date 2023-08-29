package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundKeepAlivePacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.IncomingHandler;
import lombok.NonNull;

public class ClientKeepaliveHandler implements IncomingHandler<ClientboundKeepAlivePacket, ClientSession> {
    @Override
    public boolean apply(@NonNull ClientboundKeepAlivePacket packet, @NonNull ClientSession session) {
        return false;
    }

    @Override
    public Class<ClientboundKeepAlivePacket> getPacketClass() {
        return ClientboundKeepAlivePacket.class;
    }
}
