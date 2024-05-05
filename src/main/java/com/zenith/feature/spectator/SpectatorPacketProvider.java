package com.zenith.feature.spectator;

import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Equipment;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ObjectEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Animation;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;

import java.util.List;

import static com.zenith.Shared.CACHE;
import static java.util.Arrays.asList;

/**
 * providers for cached player packets
 */
public class SpectatorPacketProvider {

    public static List<Packet> playerPosition() {
        return asList(
            new ClientboundTeleportEntityPacket(
                CACHE.getPlayerCache().getEntityId(),
                CACHE.getPlayerCache().getX(),
                CACHE.getPlayerCache().getY(),
                CACHE.getPlayerCache().getZ(),
                CACHE.getPlayerCache().getYaw(),
                CACHE.getPlayerCache().getPitch(),
                false
            ),
            new ClientboundRotateHeadPacket(
                CACHE.getPlayerCache().getEntityId(),
                CACHE.getPlayerCache().getYaw()
            ));
    }

    public static List<Packet> playerEquipment() {
        var helmet = new Equipment(EquipmentSlot.HELMET, CACHE.getPlayerCache().getEquipment(EquipmentSlot.HELMET));
        var chestplate = new Equipment(EquipmentSlot.CHESTPLATE, CACHE.getPlayerCache().getEquipment(EquipmentSlot.CHESTPLATE));
        var leggings = new Equipment(EquipmentSlot.LEGGINGS, CACHE.getPlayerCache().getEquipment(EquipmentSlot.LEGGINGS));
        var boots = new Equipment(EquipmentSlot.BOOTS, CACHE.getPlayerCache().getEquipment(EquipmentSlot.BOOTS));
        var mainHand = new Equipment(EquipmentSlot.MAIN_HAND, CACHE.getPlayerCache().getEquipment(EquipmentSlot.MAIN_HAND));
        var offHand = new Equipment(EquipmentSlot.OFF_HAND, CACHE.getPlayerCache().getEquipment(EquipmentSlot.OFF_HAND));
        return asList(
            new ClientboundSetEquipmentPacket(
                CACHE.getPlayerCache().getEntityId(),
                asList( helmet, chestplate, leggings, boots, mainHand, offHand ))
        );
    }

    public static List<Packet> playerSpawn() {
        return asList(
            new ClientboundAddEntityPacket(
                CACHE.getPlayerCache().getEntityId(),
                CACHE.getProfileCache().getProfile().getId(),
                EntityType.PLAYER,
                CACHE.getPlayerCache().getX(),
                CACHE.getPlayerCache().getY(),
                CACHE.getPlayerCache().getZ(),
                CACHE.getPlayerCache().getYaw(),
                CACHE.getPlayerCache().getPitch(),
                CACHE.getPlayerCache().getThePlayer().getHeadYaw()),
            new ClientboundSetEntityDataPacket(
                CACHE.getPlayerCache().getEntityId(),
                CACHE.getPlayerCache().getThePlayer().getMetadata())
        );
    }

    public static List<Packet> playerSneak() {
        return asList(
            new ClientboundSetEntityDataPacket(
                CACHE.getPlayerCache().getEntityId(),
                asList(new ObjectEntityMetadata<>(6, MetadataType.POSE, CACHE.getPlayerCache().isSneaking() ? Pose.SNEAKING : Pose.STANDING)))
        );
    }

    public static List<Packet> playerSwing() {
        return asList(
            new ClientboundAnimatePacket(
                CACHE.getPlayerCache().getEntityId(),
                Animation.SWING_ARM
            )
        );
    }
}
