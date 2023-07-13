package com.zenith.network.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityEffectPacket;
import com.zenith.cache.data.entity.EntityEquipment;
import com.zenith.cache.data.entity.PotionEffect;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CLIENT_LOG;

public class EntityEffectHandler implements AsyncIncomingHandler<ServerEntityEffectPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerEntityEffectPacket packet, @NonNull ClientSession session) {
        try {
            EntityEquipment entity = CACHE.getEntityCache().get(packet.getEntityId());
            if (entity != null) {
                entity.getPotionEffectMap().put(packet.getEffect(), new PotionEffect(
                        packet.getEffect(),
                        packet.getAmplifier(),
                        packet.getDuration(),
                        packet.isAmbient(),
                        packet.getShowParticles()
                ));
            } else {
                CLIENT_LOG.warn("Received ServerEntityEffectPacket for invalid entity (id={})", packet.getEntityId());
                return false;
            }
        } catch (ClassCastException e)  {
            CLIENT_LOG.warn("Received ServerEntityEffectPacket for non-equipment entity (id={})", packet.getEntityId(), e);
            return false;
        }
        return true;
    }

    @Override
    public Class<ServerEntityEffectPacket> getPacketClass() {
        return ServerEntityEffectPacket.class;
    }
}
