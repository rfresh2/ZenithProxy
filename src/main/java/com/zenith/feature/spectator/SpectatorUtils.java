package com.zenith.feature.spectator;

import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Equipment;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.*;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundRotateHeadPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEquipmentPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundTeleportEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerAbilitiesPacket;
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
                new ByteEntityMetadata(0, MetadataType.BYTE, (byte) (((byte) 0) | 0x20)),
                new IntEntityMetadata(1, MetadataType.INT, 0),
                new ObjectEntityMetadata<>(2, MetadataType.STRING, ""),
                new BooleanEntityMetadata(3, MetadataType.BOOLEAN, false),
                new BooleanEntityMetadata(4, MetadataType.BOOLEAN, false),
                new BooleanEntityMetadata(5, MetadataType.BOOLEAN, false),
                new ByteEntityMetadata(6, MetadataType.BYTE, (byte) 0),
                new FloatEntityMetadata(7, MetadataType.FLOAT, 20f),
                new IntEntityMetadata(8, MetadataType.INT, 0),
                new BooleanEntityMetadata(9, MetadataType.BOOLEAN, false),
                new IntEntityMetadata(10, MetadataType.INT, 0),
                new FloatEntityMetadata(11, MetadataType.FLOAT, 0.0f),
                new IntEntityMetadata(12, MetadataType.INT, 202),
                new ByteEntityMetadata(13, MetadataType.BYTE, (byte) 0),
                new ByteEntityMetadata(14, MetadataType.BYTE, (byte) 1),
                new ObjectEntityMetadata<>(15, MetadataType.NBT_TAG, emptyNbtTag),
                new ObjectEntityMetadata<>(16, MetadataType.NBT_TAG, emptyNbtTag)));
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
            .setMaxPlayers(CACHE.getPlayerCache().getMaxPlayers());
        session.setAllowSpectatorServerPlayerPosRotate(true);
        DataCache.sendCacheData(cacheSupplier.get(), session);
        // todo: see LoginHandler for additional packets we need to send
        //  currently unable to see self-entity
        //  also not currently in spectator gamemode
        session.send(session.getEntitySpawnPacket());
        session.send(session.getSelfEntityMetadataPacket());
        session.getProxy().getActiveConnections().stream()
                .filter(connection -> !connection.equals(session))
                .forEach(connection -> spawnSpectatorForOtherSessions(session, connection));
        session.send(new ClientboundPlayerAbilitiesPacket(true, true, true, false, 0.05f, 0.1f));
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
