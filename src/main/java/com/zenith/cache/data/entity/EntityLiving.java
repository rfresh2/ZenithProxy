package com.zenith.cache.data.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Equipment;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEquipmentPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundUpdateMobEffectPacket;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class EntityLiving extends Entity {
    @Nullable
    protected Float health;
    protected Map<Effect, PotionEffect> potionEffectMap = new EnumMap<>(Effect.class);
    protected Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);

    @Override
    public void addPackets(final @NotNull Consumer<Packet> consumer) {
        if (!potionEffectMap.isEmpty()) {
            this.getPotionEffectMap().forEach((effect, potionEffect) -> consumer.accept(new ClientboundUpdateMobEffectPacket(
                this.entityId,
                effect,
                potionEffect.getAmplifier(),
                potionEffect.getDuration(),
                potionEffect.isAmbient(),
                potionEffect.isShowParticles(),
                potionEffect.isShowIcon(),
                potionEffect.getFactorData()
            )));
        }
        if (!isSelfPlayer() && !getEquipment().isEmpty()) {
            consumer.accept(new ClientboundSetEquipmentPacket(this.entityId, getEquipment().entrySet().stream()
                .map(entry -> new Equipment(entry.getKey(), entry.getValue()))
                .toList()));
        }
        super.addPackets(consumer);
    }

    private boolean isSelfPlayer() {
        return this instanceof EntityPlayer player && player.isSelfPlayer();
    }
}
