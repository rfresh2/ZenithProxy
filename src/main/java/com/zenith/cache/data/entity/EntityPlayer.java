package com.zenith.cache.data.entity;

import com.github.steveice10.mc.protocol.data.game.entity.Effect;
import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Equipment;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEquipmentPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundUpdateMobEffectPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerCombatKillPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundSetExperiencePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHealthPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddPlayerPacket;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.*;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.zenith.Shared.SERVER_LOG;


@RequiredArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
public class EntityPlayer extends Entity {
    @NonNull
    protected boolean selfPlayer;

    protected int food;
    protected float saturation;
    protected int totalExperience;
    protected int level;
    protected float experience;
    protected Map<Effect, PotionEffect> potionEffectMap = new EnumMap<>(Effect.class);
    protected Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
    @Nullable
    protected Float health;

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

    public void setHealth(float health) {
        this.health = health;
        final List<EntityMetadata> md = new ArrayList<>(this.getMetadata());
        md.forEach(meta -> {
            if (meta.getId() == 7) { // https://c4k3.github.io/wiki.vg/Entities.html#Living
                meta.setValue(health);
            }
        });
        this.metadata = md;
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
            if (!equipment.isEmpty()) {
                // skip sending equipment packets for current player because we already send this in SetWindowsItem packet
                consumer.accept(new ClientboundSetEquipmentPacket(this.entityId, this.equipment.entrySet().stream()
                    .map(entry -> new Equipment(entry.getKey(), entry.getValue()))
                    .toList()
                    .toArray(new Equipment[0])));
            }
            consumer.accept(new ClientboundSetEntityDataPacket(this.entityId, this.getMetadata().toArray(new EntityMetadata[0])));
        }
        if (!potionEffectMap.isEmpty()) {
            this.getPotionEffectMap().forEach((effect, potionEffect) -> consumer.accept(new ClientboundUpdateMobEffectPacket(
                this.entityId,
                effect,
                potionEffect.getAmplifier(),
                potionEffect.getDuration(),
                potionEffect.isAmbient(),
                potionEffect.isShowParticles(),
                null // todo: cache this
            )));
        }
        super.addPackets(consumer);
    }
}
