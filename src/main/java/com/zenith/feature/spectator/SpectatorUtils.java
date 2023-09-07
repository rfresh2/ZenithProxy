package com.zenith.feature.spectator;

import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Equipment;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.FloatEntityMetadata;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundRotateHeadPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEquipmentPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundTeleportEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddPlayerPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.zenith.Proxy;
import com.zenith.cache.CachedData;
import com.zenith.cache.DataCache;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.network.server.ServerConnection;

import java.util.Collection;
import java.util.function.Supplier;

import static com.github.steveice10.mc.protocol.data.game.entity.player.GameMode.SPECTATOR;
import static com.zenith.Shared.CACHE;
import static java.util.Arrays.asList;

public final class SpectatorUtils {

    public static void syncPlayerEquipmentWithSpectatorsFromCache() {
        sendSpectatorsEquipment(EquipmentSlot.OFF_HAND);
        sendSpectatorsEquipment(EquipmentSlot.HELMET);
        sendSpectatorsEquipment(EquipmentSlot.CHESTPLATE);
        sendSpectatorsEquipment(EquipmentSlot.LEGGINGS);
        sendSpectatorsEquipment(EquipmentSlot.BOOTS);
        sendSpectatorsEquipment(EquipmentSlot.MAIN_HAND);
    }

    public static void syncPlayerPositionWithSpectators() {
        Proxy.getInstance().getSpectatorConnections().forEach(connection -> {
            connection.send(new ClientboundTeleportEntityPacket(
                    CACHE.getPlayerCache().getEntityId(),
                    CACHE.getPlayerCache().getX(),
                    CACHE.getPlayerCache().getY(),
                    CACHE.getPlayerCache().getZ(),
                    CACHE.getPlayerCache().getYaw(),
                    CACHE.getPlayerCache().getPitch(),
                    false // idk if this will break any rendering or not
            ));
            connection.send(new ClientboundRotateHeadPacket(
                    CACHE.getPlayerCache().getEntityId(),
                    CACHE.getPlayerCache().getYaw()
            ));
        });
    }

    public static void syncSpectatorPositionToPlayer(final ServerConnection spectConnection) {
        spectConnection.setAllowSpectatorServerPlayerPosRotate(true);
        spectConnection.send(new ClientboundPlayerPositionPacket(
                CACHE.getPlayerCache().getX(),
                CACHE.getPlayerCache().getY(),
                CACHE.getPlayerCache().getZ(),
                CACHE.getPlayerCache().getYaw(),
                CACHE.getPlayerCache().getPitch(),
                12345678
        ));
        spectConnection.setAllowSpectatorServerPlayerPosRotate(false);
        Proxy.getInstance().getActiveConnections().forEach(c -> {
            if (!c.equals(spectConnection) || spectConnection.isShowSelfEntity()) {
                c.send(spectConnection.getEntitySpawnPacket());
                c.send(spectConnection.getEntityMetadataPacket());
            }
        });
    }

    private static void sendSpectatorsEquipment(final EquipmentSlot equipmentSlot) {
        Proxy.getInstance().getSpectatorConnections().forEach(connection -> {
            sendSpectatorsEquipment(connection, equipmentSlot);
        });
    }

    private static void sendSpectatorsEquipment(final ServerConnection connection, final EquipmentSlot equipmentSlot) {
        connection.send(new ClientboundSetEquipmentPacket(
                CACHE.getPlayerCache().getEntityId(),
                CACHE.getPlayerCache().getThePlayer().getEquipment().entrySet().stream()
                    .map(entry -> new Equipment(entry.getKey(), entry.getValue()))
                    .toArray(Equipment[]::new)));
    }

    private static void spawnSpectatorForOtherSessions(ServerConnection session, ServerConnection connection) {
        if (connection.equals(session.getProxy().getCurrentPlayer().get())) {
            session.send(new ClientboundAddPlayerPacket(
                    CACHE.getPlayerCache().getEntityId(),
                    CACHE.getProfileCache().getProfile().getId(),
                    CACHE.getPlayerCache().getX(),
                    CACHE.getPlayerCache().getY(),
                    CACHE.getPlayerCache().getZ(),
                    CACHE.getPlayerCache().getYaw(),
                    CACHE.getPlayerCache().getPitch()));
            session.send(new ClientboundSetEntityDataPacket(
                    CACHE.getPlayerCache().getEntityId(),
                    CACHE.getPlayerCache().getThePlayer().getEntityMetadataAsArray()));
        } else {
            session.send(connection.getEntitySpawnPacket());
            session.send(connection.getEntityMetadataPacket());
        }
        connection.send(session.getEntitySpawnPacket());
        connection.send(session.getEntityMetadataPacket());
        SpectatorUtils.syncPlayerEquipmentWithSpectatorsFromCache();
    }

    public static EntityPlayer getSpectatorPlayerEntity(final ServerConnection session) {
        EntityPlayer spectatorEntityPlayer = new EntityPlayer();
        spectatorEntityPlayer.setUuid(session.getProfileCache().getProfile().getId());
        spectatorEntityPlayer.setSelfPlayer(true);
        spectatorEntityPlayer.setX(CACHE.getPlayerCache().getX());
        spectatorEntityPlayer.setY(CACHE.getPlayerCache().getY());
        spectatorEntityPlayer.setZ(CACHE.getPlayerCache().getZ());
        spectatorEntityPlayer.setEntityId(session.getSpectatorSelfEntityId());
        spectatorEntityPlayer.setYaw(CACHE.getPlayerCache().getYaw());
        spectatorEntityPlayer.setPitch(CACHE.getPlayerCache().getPitch());
        final CompoundTag emptyNbtTag = new CompoundTag("");
        emptyNbtTag.clear();
        spectatorEntityPlayer.setMetadata(asList(
            new FloatEntityMetadata(9, MetadataType.FLOAT, 20.0f), // health
            new ByteEntityMetadata(17, MetadataType.BYTE, (byte) 255) // visible skin parts
        ));
        return spectatorEntityPlayer;
    }

    public static void initSpectator(ServerConnection session, Supplier<Collection<CachedData>> cacheSupplier) {
        // update spectator player cache
        EntityPlayer spectatorEntityPlayer = getSpectatorPlayerEntity(session);
        session.getSpectatorPlayerCache()
            .setThePlayer(spectatorEntityPlayer)
            .setGameMode(SPECTATOR)
            .setEnableRespawnScreen(CACHE.getPlayerCache().isEnableRespawnScreen())
            .setLastDeathPos(CACHE.getPlayerCache().getLastDeathPos())
            .setPortalCooldown(CACHE.getPlayerCache().getPortalCooldown())
            .setHardcore(CACHE.getPlayerCache().isHardcore())
            .setReducedDebugInfo(CACHE.getPlayerCache().isReducedDebugInfo())
            .setHeldItemSlot(CACHE.getPlayerCache().getHeldItemSlot())
            .setEnabledFeatures(CACHE.getPlayerCache().getEnabledFeatures())
            .setDifficultyLocked(CACHE.getPlayerCache().isDifficultyLocked())
            .setInvincible(true)
            .setCanFly(true)
            .setCreative(false)
            .setFlying(true)
            .setFlySpeed(0.05f)
            .setWalkSpeed(0.1f)
            .setTags(CACHE.getPlayerCache().getTags())
            .setOpLevel(CACHE.getPlayerCache().getOpLevel())
            .setMaxPlayers(CACHE.getPlayerCache().getMaxPlayers());
        session.setAllowSpectatorServerPlayerPosRotate(true);
        DataCache.sendCacheData(cacheSupplier.get(), session);
        session.send(session.getEntitySpawnPacket());
        session.send(session.getSelfEntityMetadataPacket());
        session.getProxy().getActiveConnections().stream()
                .filter(connection -> !connection.equals(session))
                .forEach(connection -> spawnSpectatorForOtherSessions(session, connection));
        session.send(new ClientboundSetEntityDataPacket(session.getSpectatorSelfEntityId(), spectatorEntityPlayer.getEntityMetadataAsArray()));
        session.setAllowSpectatorServerPlayerPosRotate(false);
    }

    public static void checkSpectatorPositionOutOfRender(final ServerConnection spectConnection) {
        final int spectX = (int) spectConnection.getSpectatorPlayerCache().getX() >> 4;
        final int spectZ = (int) spectConnection.getSpectatorPlayerCache().getZ() >> 4;
        final int playerX = (int) CACHE.getPlayerCache().getX() >> 4;
        final int playerZ = (int) CACHE.getPlayerCache().getZ() >> 4;
        if (Math.abs(spectX - playerX) > (CACHE.getChunkCache().getRenderDistance() / 2 + 1) || Math.abs(spectZ - playerZ) > (CACHE.getChunkCache().getRenderDistance() / 2 + 1)) {
            SpectatorUtils.syncSpectatorPositionToPlayer(spectConnection);
        }
    }
}
