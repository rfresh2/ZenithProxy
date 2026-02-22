package com.zenith.feature.spectator;

import com.google.common.collect.Lists;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Equipment;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ObjectEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Animation;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.BlockBreakStage;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundBlockDestructionPacket;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.zenith.Globals.CACHE;
import static java.util.Arrays.asList;

/**
 * providers for cached player packets
 */
public class SpectatorPacketProvider {

    public static List<Packet> playerPosition() {
        return List.of(
            new ClientboundTeleportEntityPacket(
                CACHE.getPlayerCache().getEntityId(),
                CACHE.getPlayerCache().getX(),
                CACHE.getPlayerCache().getY(),
                CACHE.getPlayerCache().getZ(),
                CACHE.getPlayerCache().getVelX(),
                CACHE.getPlayerCache().getVelY(),
                CACHE.getPlayerCache().getVelZ(),
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
        return List.of(
            new ClientboundSetEquipmentPacket(
                CACHE.getPlayerCache().getEntityId(),
                asList(helmet, chestplate, leggings, boots, mainHand, offHand))
        );
    }

    public static List<Packet> playerSpawn() {
        var list = Lists.<Packet>newArrayList(
            new ClientboundAddEntityPacket(
                CACHE.getPlayerCache().getEntityId(),
                CACHE.getProfileCache().getProfile().getId(),
                EntityType.PLAYER,
                CACHE.getPlayerCache().getX(),
                CACHE.getPlayerCache().getY(),
                CACHE.getPlayerCache().getZ(),
                CACHE.getPlayerCache().getYaw(),
                CACHE.getPlayerCache().getThePlayer().getHeadYaw(),
                CACHE.getPlayerCache().getPitch()),
            new ClientboundSetEntityDataPacket(
                CACHE.getPlayerCache().getEntityId(),
                new ArrayList<>(CACHE.getPlayerCache().getThePlayer().getMetadata().values()))
        );
        if (CACHE.getPlayerCache().getThePlayer().isInVehicle()) {
            var vehicleId = CACHE.getPlayerCache().getThePlayer().getVehicleId();
            var vehicleEntity = CACHE.getEntityCache().get(vehicleId);
            if (vehicleEntity != null) {
                list.add(
                    new ClientboundSetPassengersPacket(
                        vehicleEntity.getEntityId(),
                        vehicleEntity.getPassengerIds().toIntArray())
                );
            }
        }
        return list;
    }

    public static List<Packet> playerPose() {
        return List.of(
            new ClientboundSetEntityDataPacket(
                CACHE.getPlayerCache().getEntityId(),
                newArrayList(new ObjectEntityMetadata<>(6,
                    MetadataTypes.POSE,
                    CACHE.getPlayerCache().getThePlayer().getPose())))
        );
    }

    public static List<Packet> playerSwing() {
        return List.of(
            new ClientboundAnimatePacket(
                CACHE.getPlayerCache().getEntityId(),
                Animation.SWING_ARM
            )
        );
    }

    public static List<Packet> blockBreakProgress(int blockX, int blockY, int blockZ, BlockBreakStage blockBreakStage) {
        return List.of(
            new ClientboundBlockDestructionPacket(
                CACHE.getPlayerCache().getEntityId(),
                blockX, blockY, blockZ,
                blockBreakStage
            )
        );
    }
}
