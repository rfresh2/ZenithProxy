package com.zenith.cache.data.entity;

import com.github.steveice10.mc.protocol.data.game.entity.Effect;
import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Equipment;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEquipmentPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundUpdateMobEffectPacket;
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
    @Nullable
    protected Float health;
    protected Map<Effect, PotionEffect> potionEffectMap = new EnumMap<>(Effect.class);
    protected Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);

    @Override
    public void addPackets(final Consumer<Packet> consumer) {
        if (!potionEffectMap.isEmpty()) {
            this.getPotionEffectMap().forEach((effect, potionEffect) -> consumer.accept(new ClientboundUpdateMobEffectPacket(
                this.entityId,
                effect,
                potionEffect.getAmplifier(),
                potionEffect.getDuration(),
                potionEffect.isAmbient(),
                potionEffect.isShowParticles(),
                potionEffect.getFactorData()
            )));
        }
        if (!isSelfPlayer() && !getEquipment().isEmpty()) {
            consumer.accept(new ClientboundSetEquipmentPacket(this.entityId, getEquipment().entrySet().stream()
                .map(entry -> new Equipment(entry.getKey(), entry.getValue()))
                .toList()
                .toArray(new Equipment[0])));
        }
        super.addPackets(consumer);
    }

    private boolean isSelfPlayer() {
        return this instanceof EntityPlayer player && player.isSelfPlayer();
    }
}
