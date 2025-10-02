package com.zenith.network.client.handler.incoming.spawn;

import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.cache.data.entity.EntityStandard;
import com.zenith.event.module.ServerPlayerInVisualRangeEvent;
import com.zenith.feature.whitelist.PlayerListsManager;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundAddEntityPacket;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

import static com.zenith.Globals.*;

public class AddEntityHandler implements ClientEventLoopPacketHandler<ClientboundAddEntityPacket, ClientSession> {

    @Override
    public boolean applyAsync(@NonNull ClientboundAddEntityPacket packet, @NonNull ClientSession session) {
        if (packet.getType() == EntityType.PLAYER) {
            if (!addPlayer(packet, session)) {
                CLIENT_LOG.debug("Failed adding player entity (id={})", packet.getEntityId());
            }
            return true;
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
                .setVelX(packet.getMovement().getX())
                .setVelY(packet.getMovement().getY())
                .setVelZ(packet.getMovement().getZ());
            CACHE.getEntityCache().add(entity);
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
        EVENT_BUS.postAsync(new ServerPlayerInVisualRangeEvent(playerEntry, entity));
        return true;
    }
}
