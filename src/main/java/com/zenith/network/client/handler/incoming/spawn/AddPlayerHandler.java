package com.zenith.network.client.handler.incoming.spawn;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddPlayerPacket;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.event.proxy.NewPlayerInVisualRangeEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import static com.zenith.Shared.*;

public class AddPlayerHandler implements AsyncIncomingHandler<ClientboundAddPlayerPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundAddPlayerPacket packet, @NonNull ClientSession session) {
        final EntityPlayer entity = (EntityPlayer) new EntityPlayer()
                .setEntityId(packet.getEntityId())
                .setUuid(packet.getUuid())
                .setX(packet.getX())
                .setY(packet.getY())
                .setZ(packet.getZ())
                .setYaw(packet.getYaw())
                .setPitch(packet.getPitch());
        CACHE.getEntityCache().add(entity);
        CACHE.getTabListCache().getTabList().get(packet.getUuid())
                .ifPresent(playerEntry -> {
                    EVENT_BUS.postAsync(new NewPlayerInVisualRangeEvent(playerEntry, entity));
                    if (CONFIG.client.extra.visualRangePositionTracking) {
                        if (!WHITELIST_MANAGER.isUUIDFriendWhitelisted(playerEntry.getId())) {
                            CLIENT_LOG.info("Tracking Spawn {}: {}, {}, {}", playerEntry.getName(), entity.getX(), entity.getY(), entity.getZ());
                        }
                    }
                });

        return true;
    }
}
