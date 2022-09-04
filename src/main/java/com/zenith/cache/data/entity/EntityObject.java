package com.zenith.cache.data.entity;

import com.github.steveice10.mc.protocol.data.game.entity.type.object.ObjectData;
import com.github.steveice10.mc.protocol.data.game.entity.type.object.ObjectType;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnObjectPacket;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.function.Consumer;


@Getter
@Setter
@Accessors(chain = true)
public class EntityObject extends Entity {
    protected ObjectType objectType;
    protected ObjectData data;

    @Override
    public void addPackets(Consumer<Packet> consumer) {
        consumer.accept(new ServerSpawnObjectPacket(
                this.entityId,
                this.uuid,
                this.objectType,
                this.data,
                this.x,
                this.y,
                this.z,
                this.yaw,
                this.pitch,
                this.velX,
                this.velY,
                this.velZ
        ));
        super.addPackets(consumer);
    }
}
