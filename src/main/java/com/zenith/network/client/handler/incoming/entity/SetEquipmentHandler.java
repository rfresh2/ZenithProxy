package com.zenith.network.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Equipment;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEquipmentPacket;
import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityLiving;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CLIENT_LOG;

public class SetEquipmentHandler implements AsyncIncomingHandler<ClientboundSetEquipmentPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundSetEquipmentPacket packet, @NonNull ClientSession session) {
        try {
            Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
            if (entity != null) {
                Map<EquipmentSlot, ItemStack> equipmentMap = Arrays.stream(packet.getEquipment())
                    .collect(Collectors.toMap(
                        Equipment::getSlot,
                        Equipment::getItem));
                if (entity instanceof EntityLiving e) {
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

    @Override
    public Class<ClientboundSetEquipmentPacket> getPacketClass() {
        return ClientboundSetEquipmentPacket.class;
    }
}
