package com.zenith.cache.data.entity;

import com.github.steveice10.mc.protocol.data.game.entity.Effect;
import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Equipment;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEquipmentPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundUpdateMobEffectPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

@Getter
@Setter
@Accessors(chain = true)
public class EntityLiving extends Entity {
    protected EntityType entityType;
    protected Map<Effect, PotionEffect> potionEffectMap = new EnumMap<>(Effect.class);
    protected Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
    @Nullable protected Float health;

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
        if (!equipment.isEmpty()) {
            // skip sending equipment packets for current player because we already send this in SetWindowsItem packet
            consumer.accept(new ClientboundSetEquipmentPacket(this.entityId, this.equipment.entrySet().stream()
                .map(entry -> new Equipment(entry.getKey(), entry.getValue()))
                .toList()
                .toArray(new Equipment[0])));
        }
        super.addPackets(consumer);
    }
}
