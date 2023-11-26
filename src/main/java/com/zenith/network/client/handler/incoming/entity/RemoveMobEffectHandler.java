package com.zenith.network.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundRemoveMobEffectPacket;
import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityLiving;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CLIENT_LOG;

public class RemoveMobEffectHandler implements AsyncPacketHandler<ClientboundRemoveMobEffectPacket, ClientSession> {

    @Override
    public boolean applyAsync(ClientboundRemoveMobEffectPacket packet, ClientSession session) {
        try {
            Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
            if (entity instanceof EntityLiving e) {
                e.getPotionEffectMap().remove(packet.getEffect());
            } else {
                CLIENT_LOG.warn("Received ClientboundRemoveMobEffectPacket for invalid entity (id={})", packet.getEntityId());
                return false;
            }
        } catch (Exception e) {
            CLIENT_LOG.warn("Failed handling ClientboundRemoveMobEffectPacket for entity (id={})", packet.getEntityId(), e);
        }
        return true;
    }
}
