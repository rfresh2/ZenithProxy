package com.zenith.cache.data.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Equipment;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundAddEntityPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRotateHeadPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEquipmentPacket;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class EntityStandard extends EntityLiving {

    @Override
    public void addPackets(final @NonNull Consumer<Packet> consumer) {
        consumer.accept(new ClientboundAddEntityPacket(
            this.entityId,
            this.uuid,
            this.entityType,
            this.objectData,
            this.x,
            this.y,
            this.z,
            Vector3d.from(this.velX, this.velY, this.velZ),
            this.yaw,
            this.headYaw,
            this.pitch
        ));
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
