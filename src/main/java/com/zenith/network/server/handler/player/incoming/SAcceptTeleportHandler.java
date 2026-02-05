package com.zenith.network.server.handler.player.incoming;

import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;

import static com.zenith.Globals.SERVER_LOG;

public class SAcceptTeleportHandler implements PacketHandler<ServerboundAcceptTeleportationPacket, ServerSession> {
    @Override
    public ServerboundAcceptTeleportationPacket apply(final ServerboundAcceptTeleportationPacket packet, final ServerSession session) {
        if (session.isSpawned()) return packet;
        else {
            if (session.getSpawnTeleportId() == packet.getId()) {
                SERVER_LOG.debug("[{}] Accepted spawn teleport", session.getName());
                session.setSpawning(true);
            } else {
                SERVER_LOG.debug("[{}] Cancelling unexpected pre-spawn teleport packet with ID: {}", session.getName(), packet.getId());
            }
            return null;
        }
    }
}
