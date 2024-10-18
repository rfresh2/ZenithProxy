package com.zenith.cache.data.entity;

import com.zenith.module.impl.PlayerSimulation;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.Attribute;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRotateHeadPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerCombatKillPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundSetExperiencePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHealthPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.zenith.Shared.MODULE;
import static com.zenith.Shared.SERVER_LOG;


@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class EntityPlayer extends EntityLiving {
    protected boolean selfPlayer;
    protected int food = 20;
    protected float saturation = 5;
    protected int totalExperience;
    protected int level;
    protected float experience;

    public EntityPlayer() {
        this(false);
    }

    public EntityPlayer(boolean selfPlayer) {
        //set health to maximum by default
        this.health = 20.0f;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.equipment.put(slot, null);
        }
        this.entityType = EntityType.PLAYER;
        this.selfPlayer = selfPlayer;
    }

    @Override
    public void updateAttributes(final List<Attribute> attributes) {
        super.updateAttributes(attributes);
        if (this.selfPlayer) MODULE.get(PlayerSimulation.class).updateAttributes();
    }

    public boolean isAlive() {
        return this.health > 0.0f;
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
            consumer.accept(new ClientboundAddEntityPacket(
                this.entityId,
                this.uuid,
                EntityType.PLAYER,
                this.x,
                this.y,
                this.z,
                this.yaw,
                this.pitch,
                this.headYaw
            ));
            consumer.accept(new ClientboundRotateHeadPacket(
                this.entityId,
                this.headYaw
            ));
            consumer.accept(new ClientboundSetEntityDataPacket(this.entityId, new ArrayList<>(this.getMetadata().values())));
        }
        super.addPackets(consumer);
    }
}
