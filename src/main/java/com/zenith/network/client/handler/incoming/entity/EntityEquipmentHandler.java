package com.zenith.network.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityEquipmentPacket;
import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityArmorStand;
import com.zenith.cache.data.entity.EntityEquipment;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CLIENT_LOG;

public class EntityEquipmentHandler implements AsyncIncomingHandler<ServerEntityEquipmentPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerEntityEquipmentPacket packet, @NonNull ClientSession session) {
        try {
            Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
            if (entity != null) {
                if (entity instanceof EntityEquipment e) {
                    e.getEquipment().put(packet.getSlot(), packet.getItem());
                } else if (entity instanceof EntityArmorStand e) {
                    e.getEquipment().put(packet.getSlot(), packet.getItem());
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
    public Class<ServerEntityEquipmentPacket> getPacketClass() {
        return ServerEntityEquipmentPacket.class;
    }
}
