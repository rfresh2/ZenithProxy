package com.zenith.network.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;
import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.event.proxy.PlayerLeftVisualRangeEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;
import lombok.NonNull;

import static com.zenith.Shared.*;

public class RemoveEntitiesHandler implements AsyncPacketHandler<ClientboundRemoveEntitiesPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundRemoveEntitiesPacket packet, @NonNull ClientSession session) {
        for (int id : packet.getEntityIds()) {
            try {
                if (CONFIG.client.extra.visualRangePositionTracking) {
                    final Entity entity = CACHE.getEntityCache().get(id);
                    if (entity instanceof EntityPlayer && !((EntityPlayer) entity).isSelfPlayer()) {
                        CACHE.getTabListCache().get(entity.getUuid()).ifPresentOrElse(playerEntry -> {
                            if (!WHITELIST_MANAGER.isUUIDFriendWhitelisted(playerEntry.getProfileId())) {
                                CLIENT_LOG.info("Tracking Leave {}: {}, {}, {}", playerEntry.getName(), entity.getX(), entity.getY(), entity.getZ());
                            }
                        }, () -> { // might happen if the player logs out while in visual range and tablist packet is processed first
                            if (!WHITELIST_MANAGER.isUUIDFriendWhitelisted(entity.getUuid())) {
                                CLIENT_LOG.info("Tracking Logout Leave {}: {}, {}. {}", entity.getUuid().toString(), entity.getX(), entity.getY(), entity.getZ());
                            }
                        });
                    }
                }
                Entity removed = CACHE.getEntityCache().remove(id);
                if (removed != null) {
                    for (int passenger : removed.getPassengerIds()) {
                        final Entity passengerEntity = CACHE.getEntityCache().get(passenger);
                        if (passengerEntity != null) {
                            passengerEntity.dismountVehicle();
                        }
                    }
                    if (removed instanceof EntityPlayer player && !player.isSelfPlayer()) {
                        EVENT_BUS.postAsync(new PlayerLeftVisualRangeEvent(
                            CACHE.getTabListCache()
                                .get(player.getUuid())
                                // todo: this packet seems to always be received first and we shouldn't hit the orElse, but this could change based on the server
                                .orElse(new PlayerListEntry("", player.getUuid())),
                            player
                        ));
                    }
                }
            } catch (final Exception e) {
                CLIENT_LOG.debug("Error removing entity with ID: {}", id, e);
            }
        }
        return true;
    }
}
