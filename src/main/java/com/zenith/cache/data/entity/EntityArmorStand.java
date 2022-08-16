package com.zenith.cache.data.entity;

import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityEquipmentPacket;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

@Getter
@Setter
@Accessors(chain = true)
public class EntityArmorStand extends EntityObject {
    protected Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
    {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.equipment.put(slot, null);
        }
    }
    @Override
    public void addPackets(@NonNull Consumer<Packet> consumer) {
        this.equipment.forEach((slot, stack) -> consumer.accept(new ServerEntityEquipmentPacket(this.entityId, slot, stack)));
        super.addPackets(consumer);
    }
}
