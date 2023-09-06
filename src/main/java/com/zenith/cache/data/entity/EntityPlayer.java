package com.zenith.cache.data.entity;

import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerCombatKillPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundSetExperiencePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHealthPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddPlayerPacket;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.*;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;

import java.util.function.Consumer;

import static com.zenith.Shared.SERVER_LOG;


@RequiredArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
public class EntityPlayer extends EntityLiving {
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
        this.food = 20;
        this.saturation = 5;
    }

    {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.equipment.put(slot, null);
        }
    }

    @Override
    public void addPackets(@NonNull Consumer<Packet> consumer) {
        if (this.selfPlayer) {
            consumer.accept(new ClientboundSetHealthPacket(
                    this.health,
                    this.food,
                    this.saturation
            ));
            consumer.accept(new ClientboundSetExperiencePacket(experience, level, totalExperience));
            if (this.health == 0.0f) {
                // indicates respawn screen should be shown
                SERVER_LOG.info("Sending respawn screen packet. entityId: {},", this.entityId);
                consumer.accept(new ClientboundPlayerCombatKillPacket(this.entityId, Component.text("")));
            }
        } else {
            consumer.accept(new ClientboundAddPlayerPacket(
                    this.entityId,
                    this.uuid,
                    this.x,
                    this.y,
                    this.z,
                    this.yaw,
                    this.pitch)
            );
            consumer.accept(new ClientboundSetEntityDataPacket(this.entityId, this.getMetadata().toArray(new EntityMetadata[0])));
        }
        super.addPackets(consumer);
    }
}
