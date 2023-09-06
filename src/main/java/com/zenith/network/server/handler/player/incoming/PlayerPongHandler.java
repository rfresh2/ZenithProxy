package com.zenith.network.server.handler.player.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundPongPacket;
import com.zenith.network.registry.IncomingHandler;
import com.zenith.network.server.ServerConnection;
import lombok.NonNull;

public class PlayerPongHandler implements IncomingHandler<ServerboundPongPacket, ServerConnection> {
    @Override
    public boolean apply(@NonNull ServerboundPongPacket packet, @NonNull ServerConnection session) {
        if (packet.getId() == session.getLastPingId()) {
            session.setPing(System.currentTimeMillis() - session.getLastPingTime());
        }
        return true;
    }

    @Override
    public Class<ServerboundPongPacket> getPacketClass() {
        return ServerboundPongPacket.class;
    }
}
