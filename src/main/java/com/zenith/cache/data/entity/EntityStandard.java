package com.zenith.cache.data.entity;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Equipment;
import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundRotateHeadPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEquipmentPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.function.Consumer;

@Getter
@Setter
@Accessors(chain = true)
public class EntityStandard extends EntityLiving {
    protected EntityType entityType;

    @Override
    public void addPackets(final Consumer<Packet> consumer) {
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
                .toList()
                .toArray(new Equipment[0])));
        }
        consumer.accept(new ClientboundRotateHeadPacket(
            this.entityId,
            this.headYaw
        ));
        super.addPackets(consumer);
    }
}
