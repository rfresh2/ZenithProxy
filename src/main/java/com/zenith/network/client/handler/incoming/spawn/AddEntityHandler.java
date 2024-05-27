package com.zenith.network.client.handler.incoming.spawn;

import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.cache.data.entity.EntityStandard;
import com.zenith.event.module.EntityFishHookSpawnEvent;
import com.zenith.event.proxy.NewPlayerInVisualRangeEvent;
import com.zenith.feature.whitelist.PlayerListsManager;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import lombok.NonNull;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;

import java.util.Optional;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.EVENT_BUS;

public class AddEntityHandler implements ClientEventLoopPacketHandler<ClientboundAddEntityPacket, ClientSession> {

    @Override
    public boolean applyAsync(@NonNull ClientboundAddEntityPacket packet, @NonNull ClientSession session) {
        if (packet.getType() == EntityType.PLAYER) {
            return addPlayer(packet, session);
        } else {
            final EntityStandard entity = (EntityStandard) new EntityStandard()
                .setEntityType(packet.getType())
                .setObjectData(packet.getData())
                .setEntityId(packet.getEntityId())
                .setUuid(packet.getUuid())
                .setX(packet.getX())
                .setY(packet.getY())
                .setZ(packet.getZ())
                .setYaw(packet.getYaw())
                .setPitch(packet.getPitch())
                .setHeadYaw(packet.getHeadYaw())
                .setVelX(packet.getMotionX())
                .setVelY(packet.getMotionY())
                .setVelZ(packet.getMotionZ());
            CACHE.getEntityCache().add(entity);
            if (entity.getEntityType() == EntityType.FISHING_BOBBER)
                EVENT_BUS.postAsync(new EntityFishHookSpawnEvent(entity));
        }
        return true;
    }

    private boolean addPlayer(ClientboundAddEntityPacket packet, ClientSession session) {
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
                           // alternate solution would be to just wait another tick or so for the tablist to be populated
                           PlayerListsManager.getProfileFromUUID(packet.getUuid())
                               .map(entry -> new PlayerListEntry(entry.name(), entry.uuid()))
                               .orElseGet(() -> new PlayerListEntry("", packet.getUuid())));
        EVENT_BUS.postAsync(new NewPlayerInVisualRangeEvent(playerEntry, entity));
        return true;
    }
}
