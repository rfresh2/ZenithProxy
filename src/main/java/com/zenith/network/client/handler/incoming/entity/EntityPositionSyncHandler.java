package com.zenith.network.client.handler.incoming.entity;

import com.zenith.cache.data.entity.Entity;
import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundEntityPositionSyncPacket;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.CLIENT_LOG;

public class EntityPositionSyncHandler implements ClientEventLoopPacketHandler<ClientboundEntityPositionSyncPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundEntityPositionSyncPacket packet, final ClientSession session) {
        Entity entity = CACHE.getEntityCache().get(packet.getId());
        if (entity != null) {
            // todo: need to tick entity movement according to steps
            var endX = packet.getEndPosition() != null
                ? packet.getEndPosition().getX()
                : packet.getSteps().getLast().position().getX();
            var endY = packet.getEndPosition() != null
                ? packet.getEndPosition().getY()
                : packet.getSteps().getLast().position().getY();
            var endZ = packet.getEndPosition() != null
                ? packet.getEndPosition().getZ()
                : packet.getSteps().getLast().position().getZ();
            entity
                .setX(endX)
                .setY(endY)
                .setZ(endZ)
                .setYaw(packet.getYaw())
                .setPitch(packet.getPitch());
            if (!entity.getPassengerIds().isEmpty()) {
                var player = CACHE.getPlayerCache().getThePlayer();
                if (entity.getPassengerIds().contains(player.getEntityId())) {
                    player
                        .setX(endX)
                        .setY(endY)
                        .setZ(endZ);
                    SpectatorSync.syncPlayerPositionWithSpectators();
                }
            }
            return true;
        } else {
            CLIENT_LOG.debug("Received ClientboundEntityPositionSyncPacket for invalid entity (id={})", packet.getId());
            return true;
        }
    }
}
