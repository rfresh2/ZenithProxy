package com.zenith.network.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityDestroyPacket;
import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import static com.zenith.Shared.*;

public class EntityDestroyHandler implements AsyncIncomingHandler<ServerEntityDestroyPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerEntityDestroyPacket packet, @NonNull ClientSession session) {
        for (int id : packet.getEntityIds()) {
            if (CONFIG.client.extra.visualRangePositionTracking) {
                final Entity entity = CACHE.getEntityCache().get(id);
                if (entity instanceof EntityPlayer && !((EntityPlayer) entity).isSelfPlayer()) {
                    CACHE.getTabListCache().getTabList().get(entity.getUuid()).ifPresentOrElse(playerEntry -> {
                        if (!WHITELIST_MANAGER.isUUIDFriendWhitelisted(playerEntry.getId())) {
                            CLIENT_LOG.info("Tracking Leave {}: {}, {}, {}", playerEntry.getName(), entity.getX(), entity.getY(), entity.getZ());
                        }
                    }, () -> { // might happen if the player logs out while in visual range and tablist packet is processed first
                        if (!WHITELIST_MANAGER.isUUIDFriendWhitelisted(entity.getUuid())) {
                            CLIENT_LOG.info("Tracking Logout Leave {}: {}, {}. {}", entity.getUuid().toString(), entity.getX(), entity.getY(), entity.getZ());
                        }
                    });
                }
            }
            CACHE.getEntityCache().remove(id);
        }
        return true;
    }

    @Override
    public Class<ServerEntityDestroyPacket> getPacketClass() {
        return ServerEntityDestroyPacket.class;
    }
}
