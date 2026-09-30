package com.zenith.network.client.handler.incoming.entity;

import com.zenith.cache.data.entity.Entity;
import com.zenith.feature.player.World;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import com.zenith.util.math.MathHelper;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundEntityPositionSyncPacket;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.CLIENT_LOG;

public class EntityPositionSyncHandler implements ClientEventLoopPacketHandler<ClientboundEntityPositionSyncPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundEntityPositionSyncPacket packet, final ClientSession session) {
        Entity entity = CACHE.getEntityCache().get(packet.getId());
        if (entity != null) {
            entity
                .setBaseX(packet.getX())
                .setBaseY(packet.getY())
                .setBaseZ(packet.getZ())
                .setVelX(packet.getDeltaX())
                .setVelY(packet.getDeltaY())
                .setVelZ(packet.getDeltaZ());
            var isControlledByLocalInstance = entity.isControlledByLocalInstance();
            if (!isControlledByLocalInstance) {
                var tickable = World.isChunkLoadedBlockPos(MathHelper.floorI(entity.getX()), MathHelper.floorI(entity.getZ()));
                var tooFar = entity.position().distanceSquared(packet.getX(), packet.getY(), packet.getZ()) > 4096.0;
                if (tickable && !tooFar) {
                    entity.lerpTo(packet.getX(), packet.getY(), packet.getZ(), packet.getYaw(), packet.getPitch(), 3);
                } else {
                    entity
                        .setX(packet.getX())
                        .setY(packet.getY())
                        .setZ(packet.getZ())
                        .setYaw(packet.getYaw())
                        .setPitch(packet.getPitch());
                }
            }
            return true;
        } else {
            CLIENT_LOG.debug("Received ClientboundEntityPositionSyncPacket for invalid entity (id={})", packet.getId());
            return true;
        }
    }
}
