package com.zenith.util.spectator;

import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityEquipmentPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityHeadLookPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityMetadataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityTeleportPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerAbilitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPlayerPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.zenith.Proxy;
import com.zenith.cache.CachedData;
import com.zenith.cache.DataCache;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.server.ServerConnection;

import java.util.Collection;
import java.util.function.Supplier;

import static com.github.steveice10.mc.protocol.data.game.entity.player.GameMode.SPECTATOR;
import static com.zenith.util.Constants.CACHE;
import static java.util.Arrays.asList;

public final class SpectatorHelper {

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
            connection.send(new ServerEntityTeleportPacket(
                    CACHE.getPlayerCache().getEntityId(),
                    CACHE.getPlayerCache().getX(),
                    CACHE.getPlayerCache().getY(),
                    CACHE.getPlayerCache().getZ(),
                    CACHE.getPlayerCache().getYaw(),
                    CACHE.getPlayerCache().getPitch(),
                    false // idk if this will break any rendering or not
            ));
            connection.send(new ServerEntityHeadLookPacket(
                    CACHE.getPlayerCache().getEntityId(),
                    CACHE.getPlayerCache().getYaw()
            ));
        });
    }

    private static void sendSpectatorsEquipment(final EquipmentSlot equipmentSlot) {
        Proxy.getInstance().getSpectatorConnections().forEach(connection -> {
            sendSpectatorsEquipment(connection, equipmentSlot);
        });
    }

    private static void sendSpectatorsEquipment(final ServerConnection connection, final EquipmentSlot equipmentSlot) {
        connection.send(new ServerEntityEquipmentPacket(
                CACHE.getPlayerCache().getEntityId(),
                equipmentSlot,
                CACHE.getPlayerCache().getThePlayer().getEquipment().get(equipmentSlot)
        ));
    }

    private static void spawnSpectatorForOtherSessions(ServerConnection session, ServerConnection connection) {
        if (connection.equals(session.getProxy().getCurrentPlayer().get())) {
            session.send(new ServerSpawnPlayerPacket(
                    CACHE.getPlayerCache().getEntityId(),
                    CACHE.getProfileCache().getProfile().getId(),
                    CACHE.getPlayerCache().getX(),
                    CACHE.getPlayerCache().getY(),
                    CACHE.getPlayerCache().getZ(),
                    CACHE.getPlayerCache().getYaw(),
                    CACHE.getPlayerCache().getPitch(),
                    CACHE.getPlayerCache().getThePlayer().getEntityMetadataAsArray()));
        } else {
            session.send(connection.getEntitySpawnPacket());
            session.send(connection.getEntityMetadataPacket());
        }
        connection.send(session.getEntitySpawnPacket());
        connection.send(session.getEntityMetadataPacket());
        SpectatorHelper.syncPlayerEquipmentWithSpectatorsFromCache();
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
                new EntityMetadata(0, MetadataType.BYTE, (byte) (((byte) 0) | 0x20)),
                new EntityMetadata(1, MetadataType.INT, 0),
                new EntityMetadata(2, MetadataType.STRING, ""),
                new EntityMetadata(3, MetadataType.BOOLEAN, false),
                new EntityMetadata(4, MetadataType.BOOLEAN, false),
                new EntityMetadata(5, MetadataType.BOOLEAN, false),
                new EntityMetadata(6, MetadataType.BYTE, (byte) 0),
                new EntityMetadata(7, MetadataType.FLOAT, 20f),
                new EntityMetadata(8, MetadataType.INT, 0),
                new EntityMetadata(9, MetadataType.BOOLEAN, false),
                new EntityMetadata(10, MetadataType.INT, 0),
                new EntityMetadata(11, MetadataType.FLOAT, 0.0f),
                new EntityMetadata(12, MetadataType.INT, 202),
                new EntityMetadata(13, MetadataType.BYTE, (byte) 0),
                new EntityMetadata(14, MetadataType.BYTE, (byte) 1),
                new EntityMetadata(15, MetadataType.NBT_TAG, emptyNbtTag),
                new EntityMetadata(16, MetadataType.NBT_TAG, emptyNbtTag)));
        return spectatorEntityPlayer;
    }

    public static void initSpectator(ServerConnection session, Supplier<Collection<CachedData>> cacheSupplier) {
        // update spectator player cache
        EntityPlayer spectatorEntityPlayer = getSpectatorPlayerEntity(session);
        session.getSpectatorPlayerCache()
                .setThePlayer(spectatorEntityPlayer)
                .setGameMode(SPECTATOR)
                .setDimension(CACHE.getPlayerCache().getDimension())
                .setDifficulty(CACHE.getPlayerCache().getDifficulty())
                .setHardcore(false)
                .setMaxPlayers(CACHE.getPlayerCache().getMaxPlayers());
        session.setAllowSpectatorServerPlayerPosRotate(true);
        DataCache.sendCacheData(cacheSupplier.get(), session);
        session.send(session.getEntitySpawnPacket());
        session.send(session.getSelfEntityMetadataPacket());
        session.getProxy().getActiveConnections().stream()
                .filter(connection -> !connection.equals(session))
                .forEach(connection -> spawnSpectatorForOtherSessions(session, connection));
        session.send(new ServerPlayerAbilitiesPacket(true, true, true, false, 0.05f, 0.1f));
        session.send(new ServerEntityMetadataPacket(session.getSpectatorSelfEntityId(), spectatorEntityPlayer.getEntityMetadataAsArray()));
        session.setAllowSpectatorServerPlayerPosRotate(false);
    }
}
