package com.zenith.cache.data.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Equipment;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRotateHeadPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEquipmentPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class EntityStandard extends EntityLiving {

    @Override
    public void addPackets(final @NotNull Consumer<Packet> consumer) {
        consumer.accept(new ClientboundAddEntityPacket(
            this.entityId,
            this.uuid,
            this.entityType,
            this.objectData,
            this.x,
            this.y,
            this.z,
            this.yaw,
            this.pitch,
            this.headYaw,
            this.velX,
            this.velY,
            this.velZ));
        if (!equipment.isEmpty()) {
            consumer.accept(new ClientboundSetEquipmentPacket(this.entityId, this.equipment.entrySet().stream()
                .map(entry -> new Equipment(entry.getKey(), entry.getValue()))
                .toList()));
        }
        consumer.accept(new ClientboundRotateHeadPacket(
            this.entityId,
            this.headYaw
        ));
        super.addPackets(consumer);
    }
}
