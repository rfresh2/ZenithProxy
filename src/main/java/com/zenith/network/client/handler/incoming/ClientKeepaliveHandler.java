package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundKeepAlivePacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import lombok.NonNull;

public class ClientKeepaliveHandler implements PacketHandler<ClientboundKeepAlivePacket, ClientSession> {
    @Override
    public ClientboundKeepAlivePacket apply(@NonNull ClientboundKeepAlivePacket packet, @NonNull ClientSession session) {
        return null;
    }
}
