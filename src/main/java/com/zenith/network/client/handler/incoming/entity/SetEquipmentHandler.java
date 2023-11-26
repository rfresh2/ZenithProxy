package com.zenith.network.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Equipment;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEquipmentPacket;
import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityLiving;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CLIENT_LOG;

public class SetEquipmentHandler implements AsyncPacketHandler<ClientboundSetEquipmentPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundSetEquipmentPacket packet, @NonNull ClientSession session) {
        try {
            Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
            if (entity != null) {
                if (entity instanceof EntityLiving e) {
                    final Map<EquipmentSlot, ItemStack> equipmentMap = new HashMap<>();
                    for (Equipment equipment : packet.getEquipment()) {
                        if (equipment.getItem() != null) {
                            equipmentMap.put(equipment.getSlot(), equipment.getItem());
                        } else {
                            equipmentMap.put(equipment.getSlot(), null);
                        }
                    }
                    e.setEquipment(equipmentMap);
                }
            } else {
                // can occur often due to packet ordering. non-critical to retry
                CLIENT_LOG.debug("Received ServerEntityEquipmentPacket for invalid entity (id={})", packet.getEntityId());
                return false;
            }
            return true;
        } catch (ClassCastException e)  {
            CLIENT_LOG.warn("Received ServerEntityEquipmentPacket for non-equipment entity (id={})", packet.getEntityId(), e);
            return false;
        }
    }

}
