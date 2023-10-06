package com.zenith.network.client.handler.incoming.spawn;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddPlayerPacket;
import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.event.proxy.NewPlayerInVisualRangeEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import java.util.Optional;

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
        final Entity playerCachedAlready = CACHE.getEntityCache().get(packet.getEntityId());
        CACHE.getEntityCache().add(entity);
        Optional<PlayerListEntry> foundPlayerEntry = CACHE.getTabListCache().get(packet.getUuid());
        if (foundPlayerEntry.isEmpty() && playerCachedAlready == null) return false;
        PlayerListEntry playerEntry = foundPlayerEntry
            .orElseGet(() ->
                // may occur at login if this packet is received before the tablist is populated
                // this function performs a mojang api call so it will take awhile
                WHITELIST_MANAGER.getWhitelistEntryFromUUID(packet.getUuid())
                    .map(entry -> new PlayerListEntry(entry.username, entry.uuid))
                    .orElseGet(() -> new PlayerListEntry("", packet.getUuid())));
        EVENT_BUS.postAsync(new NewPlayerInVisualRangeEvent(playerEntry, entity));
        if (CONFIG.client.extra.visualRangePositionTracking && !WHITELIST_MANAGER.isUUIDFriendWhitelisted(playerEntry.getProfileId())) {
            CLIENT_LOG.info("Tracking Spawn {}: {}, {}, {}", playerEntry.getName(), entity.getX(), entity.getY(), entity.getZ());
        }
        return true;
    }
}
