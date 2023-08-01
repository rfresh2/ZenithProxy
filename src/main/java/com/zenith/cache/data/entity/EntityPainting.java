package com.zenith.cache.data.entity;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.entity.type.PaintingType;
import com.github.steveice10.mc.protocol.data.game.entity.type.object.HangingDirection;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPaintingPacket;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.function.Consumer;

@Getter
@Setter
@Accessors(chain = true)
public class EntityPainting extends Entity {
    protected PaintingType paintingType;
    protected HangingDirection direction;

    @Override
    public void addPackets(Consumer<Packet> consumer) {
        consumer.accept(new ServerSpawnPaintingPacket(
                this.entityId,
                this.uuid,
                this.paintingType,
                new Position(
                    (int) Math.floor(this.x),
                    (int) Math.floor(this.y),
                    (int) Math.floor(this.z)
                ),
                this.direction
        ));
        super.addPackets(consumer);
    }
}
