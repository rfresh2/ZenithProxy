package com.zenith.cache.data.entity;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.type.MobType;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnMobPacket;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.function.Consumer;


@Getter
@Setter
@Accessors(chain = true)
public class EntityMob extends EntityEquipment {
    protected MobType mobType;

    @Override
    public void addPackets(Consumer<Packet> consumer) {
        consumer.accept(new ServerSpawnMobPacket(
                this.entityId,
                this.uuid,
                this.mobType,
                this.x,
                this.y,
                this.z,
                this.yaw,
                this.pitch,
                this.headYaw,
                this.velX,
                this.velY,
                this.velZ,
                this.metadata.toArray(new EntityMetadata[0])
        ));
        super.addPackets(consumer);
    }
}
