package com.zenith.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityEffectPacket;
import com.zenith.cache.data.entity.EntityEquipment;
import com.zenith.cache.data.entity.PotionEffect;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.util.Constants.CACHE;
import static com.zenith.util.Constants.CLIENT_LOG;

public class EntityEffectHandler implements HandlerRegistry.AsyncIncomingHandler<ServerEntityEffectPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerEntityEffectPacket packet, @NonNull ClientSession session) {
        try {
            EntityEquipment entity = CACHE.getEntityCache().get(packet.getEntityId());
            if (entity != null) {
                entity.getPotionEffects().add(new PotionEffect(
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
            CLIENT_LOG.warn("Received ServerEntityEffectPacket for non-equipment entity (id={})", e, packet.getEntityId());
        }
        return true;
    }

    @Override
    public Class<ServerEntityEffectPacket> getPacketClass() {
        return ServerEntityEffectPacket.class;
    }
}
