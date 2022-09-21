package com.zenith.cache.data.entity;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerCombatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerHealthPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerSetExperiencePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPlayerPacket;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.function.Consumer;

import static com.zenith.util.Constants.SERVER_LOG;


@RequiredArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
public class EntityPlayer extends EntityEquipment {
    @NonNull
    protected boolean selfPlayer;

    protected int food;
    protected float saturation;
    protected int totalExperience;
    protected int level;
    protected float experience;

    {
        //set health to maximum by default
        this.health = 20.0f;
    }

    @Override
    public void addPackets(@NonNull Consumer<Packet> consumer) {
        if (this.selfPlayer) {
            consumer.accept(new ServerPlayerHealthPacket(
                    this.health,
                    this.food,
                    this.saturation
            ));
            consumer.accept(new ServerPlayerSetExperiencePacket(experience, level, totalExperience));
            if (this.health == 0.0f) {
                // indicates respawn screen should be shown
                SERVER_LOG.info("Sending respawn screen packet. entityId: {},", this.entityId);
                consumer.accept(new ServerCombatPacket(this.entityId, -1, "", false));
            }
        } else {
            consumer.accept(new ServerSpawnPlayerPacket(
                    this.entityId,
                    this.uuid,
                    this.x,
                    this.y,
                    this.z,
                    this.yaw,
                    this.pitch,
                    this.metadata.toArray(new EntityMetadata[0])
            ));
        }
        super.addPackets(consumer);
    }
}
