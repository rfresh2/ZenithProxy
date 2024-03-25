package com.zenith.network.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Equipment;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEquipmentPacket;
import com.zenith.cache.data.entity.EntityLiving;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CLIENT_LOG;

public class SetEquipmentHandler implements ClientEventLoopPacketHandler<ClientboundSetEquipmentPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundSetEquipmentPacket packet, @NonNull ClientSession session) {
        var entity = CACHE.getEntityCache().get(packet.getEntityId());
        if (entity == null) {
            CLIENT_LOG.debug("Received ServerEntityEquipmentPacket for invalid entity (id={})", packet.getEntityId());
            return false;
        }
        if (entity instanceof EntityLiving e) {
            var equipmentMap = e.getEquipment();
            for (Equipment equipment : packet.getEquipment()) {
                equipmentMap.put(equipment.getSlot(), equipment.getItem());
            }
        }
        return true;
    }

}
