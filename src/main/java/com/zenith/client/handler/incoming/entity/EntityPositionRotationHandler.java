package com.zenith.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityPositionRotationPacket;
import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.client.ClientSession;
import com.zenith.feature.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.Shared.*;

public class EntityPositionRotationHandler implements HandlerRegistry.AsyncIncomingHandler<ServerEntityPositionRotationPacket, ClientSession> {
    public static void trackPlayerVisualRangePosition(final Entity entity) {
        if (CONFIG.client.extra.visualRangePositionTracking && entity instanceof EntityPlayer && !((EntityPlayer) entity).isSelfPlayer()) {
            CACHE.getTabListCache().getTabList().get(entity.getUuid()).ifPresent(playerEntry -> {
                if (!WHITELIST_MANAGER.isUUIDFriendWhitelisted(playerEntry.getId())) {
                    CLIENT_LOG.info("Tracking {}: {}, {}, {}", playerEntry.getName(), entity.getX(), entity.getY(), entity.getZ());
                }
            });
        }
    }

    @Override
    public Class<ServerEntityPositionRotationPacket> getPacketClass() {
        return ServerEntityPositionRotationPacket.class;
    }

    @Override
    public boolean applyAsync(@NonNull ServerEntityPositionRotationPacket packet, @NonNull ClientSession session) {
        Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
        if (entity != null) {
            entity.setYaw(packet.getYaw())
                    .setPitch(packet.getPitch())
                    .setX(entity.getX() + packet.getMovementX())
                    .setY(entity.getY() + packet.getMovementY())
                    .setZ(entity.getZ() + packet.getMovementZ());
            trackPlayerVisualRangePosition(entity);
            return true;
        } else {
            CLIENT_LOG.warn("Received ServerEntityPositionRotationPacket for invalid entity (id={})", packet.getEntityId());
            return false;
        }
    }
}
