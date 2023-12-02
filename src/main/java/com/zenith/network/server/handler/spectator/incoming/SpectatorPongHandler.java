package com.zenith.network.server.handler.spectator.incoming;

import com.github.steveice10.mc.protocol.packet.common.serverbound.ServerboundPongPacket;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import lombok.NonNull;

public class SpectatorPongHandler implements PacketHandler<ServerboundPongPacket, ServerConnection> {
    @Override
    public ServerboundPongPacket apply(@NonNull ServerboundPongPacket packet, @NonNull ServerConnection session) {
        if (packet.getId() == session.getLastPingId()) {
            session.setPing(System.currentTimeMillis() - session.getLastPingTime());
        }
        return null;
    }
}
