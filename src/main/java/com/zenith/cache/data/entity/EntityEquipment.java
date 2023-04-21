package com.zenith.cache.data.entity;

import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityEffectPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityEquipmentPacket;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.*;
import java.util.function.Consumer;


@Getter
@Setter
@Accessors(chain = true)
public abstract class EntityEquipment extends Entity {
    protected List<PotionEffect> potionEffects = Collections.synchronizedList(new ArrayList<>());
    protected Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
    protected float health;

    public void setHealth(float health) {
        this.health = health;
        if (this instanceof EntityPlayer) {
            final List<EntityMetadata> md = new ArrayList<>(this.getMetadata());
            md.forEach(meta -> {
                if (meta.getId() == 7) { // https://c4k3.github.io/wiki.vg/Entities.html#Living
                    meta.setValue(health);
                }
            });
            this.metadata = md;
        }
    }

    {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.equipment.put(slot, null);
        }
    }

    @Override
    public void addPackets(@NonNull Consumer<Packet> consumer) {
        if (this instanceof EntityPlayer e) {
            if (!e.isSelfPlayer()) {
                // skip sending potion effects for self player out of precaution for potential cache desync
                // todo: come back and fix this
                this.potionEffects.forEach(effect -> consumer.accept(new ServerEntityEffectPacket(
                        this.entityId,
                        effect.getEffect(),
                        effect.getAmplifier(),
                        effect.getDuration(),
                        effect.isAmbient(),
                        effect.isShowParticles()
                )));

                // skip sending equipment packets for current player because we already send this in SetWindowsItem packet
                this.equipment.forEach((slot, stack) -> consumer.accept(new ServerEntityEquipmentPacket(this.entityId, slot, stack)));
            }
        }
        super.addPackets(consumer);
    }
}
