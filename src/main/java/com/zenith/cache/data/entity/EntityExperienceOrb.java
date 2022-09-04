package com.zenith.cache.data.entity;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnExpOrbPacket;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.function.Consumer;


@Getter
@Setter
@Accessors(chain = true)
public class EntityExperienceOrb extends Entity {
    protected int exp;

    @Override
    public void addPackets(@NonNull Consumer<Packet> consumer) {
        consumer.accept(new ServerSpawnExpOrbPacket(
                this.getEntityId(),
                this.getX(),
                this.getY(),
                this.getZ(),
                this.exp
        ));
        super.addPackets(consumer);
    }
}
