package com.zenith.network.client.handler.incoming.entity;

import com.zenith.cache.data.entity.EntityLiving;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import lombok.NonNull;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Equipment;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEquipmentPacket;

import static com.zenith.Shared.CACHE;

public class SetEquipmentHandler implements ClientEventLoopPacketHandler<ClientboundSetEquipmentPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundSetEquipmentPacket packet, @NonNull ClientSession session) {
        var entity = CACHE.getEntityCache().get(packet.getEntityId());
        if (entity == null) return false;
        if (entity instanceof EntityLiving e) {
            var equipmentMap = e.getEquipment();
            for (Equipment equipment : packet.getEquipment()) {
                equipmentMap.put(equipment.getSlot(), equipment.getItem());
            }
        }
        return true;
    }

}
