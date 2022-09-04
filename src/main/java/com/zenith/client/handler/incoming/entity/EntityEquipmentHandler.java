package com.zenith.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityEquipmentPacket;
import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityArmorStand;
import com.zenith.cache.data.entity.EntityEquipment;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.util.Constants.CACHE;
import static com.zenith.util.Constants.CLIENT_LOG;

public class EntityEquipmentHandler implements HandlerRegistry.AsyncIncomingHandler<ServerEntityEquipmentPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerEntityEquipmentPacket packet, @NonNull ClientSession session) {
        try {
            Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
            if (entity != null) {
                if (entity instanceof EntityEquipment) {
                    ((EntityEquipment) entity).getEquipment().put(packet.getSlot(), packet.getItem());
                } else if (entity instanceof EntityArmorStand) {
                    ((EntityArmorStand) entity).getEquipment().put(packet.getSlot(), packet.getItem());
                }
            } else {
                CLIENT_LOG.warn("Received ServerEntityEquipmentPacket for invalid entity (id=%d)", packet.getEntityId());
                return false;
            }
            return true;
        } catch (ClassCastException e)  {
            CLIENT_LOG.warn("Received ServerEntityEquipmentPacket for non-equipment entity (id=%d)", e, packet.getEntityId());
            return false;
        }
    }

    @Override
    public Class<ServerEntityEquipmentPacket> getPacketClass() {
        return ServerEntityEquipmentPacket.class;
    }
}
