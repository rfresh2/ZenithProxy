package com.zenith.network.client.handler.incoming.spawn;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPlayerPacket;
import com.google.common.collect.Lists;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.event.proxy.NewPlayerInVisualRangeEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import static com.zenith.Shared.*;

public class SpawnPlayerHandler implements AsyncIncomingHandler<ServerSpawnPlayerPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerSpawnPlayerPacket packet, @NonNull ClientSession session) {
        final EntityPlayer entity = (EntityPlayer) new EntityPlayer()
                .setEntityId(packet.getEntityId())
                .setUuid(packet.getUUID())
                .setX(packet.getX())
                .setY(packet.getY())
                .setZ(packet.getZ())
                .setYaw(packet.getYaw())
                .setPitch(packet.getPitch())
                .setMetadata(Lists.newArrayList(packet.getMetadata()));
        CACHE.getEntityCache().add(entity);
        CACHE.getTabListCache().getTabList().get(packet.getUUID())
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

    @Override
    public Class<ServerSpawnPlayerPacket> getPacketClass() {
        return ServerSpawnPlayerPacket.class;
    }
}
