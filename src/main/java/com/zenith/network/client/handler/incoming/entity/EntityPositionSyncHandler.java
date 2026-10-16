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
            var pos = packet.isStepped()
                ? packet.getSteps().getLast().position()
                : packet.getEndPosition();
            entity
                .setBaseX(pos.getX())
                .setBaseY(pos.getY())
                .setBaseZ(pos.getZ());
            var isControlledByLocalInstance = entity.isControlledByLocalInstance();
            if (!isControlledByLocalInstance) {
                var tickable = World.isChunkLoadedBlockPos(MathHelper.floorI(entity.getX()), MathHelper.floorI(entity.getZ()));
                var tooFar = entity.position().distanceSquared(pos.getX(), pos.getY(), pos.getZ()) > 4096.0;
                if (tickable && !tooFar) {
                    // todo: this is no longer correct in 26.3
                    //  interpolation is no longer hardcoded to 3 ticks
                    //  and each entity type has an interval over which they step through interpolations
                    entity.lerpTo(pos.getX(), pos.getY(), pos.getZ(), packet.getYaw(), packet.getPitch(), 3);
                } else {
                    entity.cancelLerp();
                    entity
                        .setX(pos.getX())
                        .setY(pos.getY())
                        .setZ(pos.getZ())
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
