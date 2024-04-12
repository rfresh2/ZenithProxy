package com.zenith.network.server.handler.shared.incoming;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import lombok.NonNull;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundPongPacket;

public class PongHandler implements PacketHandler<ServerboundPongPacket, ServerConnection> {
    @Override
    public ServerboundPongPacket apply(@NonNull ServerboundPongPacket packet, @NonNull ServerConnection session) {
        if (packet.getId() == session.getLastPingId()) {
            session.setPing(System.currentTimeMillis() - session.getLastPingTime());
        }
        return packet;
    }
}
