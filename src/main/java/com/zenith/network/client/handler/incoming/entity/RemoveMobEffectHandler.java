package com.zenith.network.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundRemoveMobEffectPacket;
import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityLiving;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CLIENT_LOG;

public class RemoveMobEffectHandler implements AsyncIncomingHandler<ClientboundRemoveMobEffectPacket, ClientSession> {

    @Override
    public boolean applyAsync(ClientboundRemoveMobEffectPacket packet, ClientSession session) {
        try {
            Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
            if (entity != null) {
                // todo: rethink class inheritance
                if (entity instanceof EntityLiving) {
                    ((EntityLiving) entity).getPotionEffectMap().remove(packet.getEffect());
                } else if (entity instanceof EntityPlayer) {
                    ((EntityPlayer) entity).getPotionEffectMap().remove(packet.getEffect());
                }
            } else {
                CLIENT_LOG.warn("Received ServerEntityRemoveEffectPacket for invalid entity (id={})", packet.getEntityId());
                return false;
            }
        } catch (ClassCastException e) {
            CLIENT_LOG.warn("Received ServerEntityRemoveEffectPacket for non-equipment entity (id={})", e, packet.getEntityId());
        }
        return true;
    }

    @Override
    public Class<ClientboundRemoveMobEffectPacket> getPacketClass() {
        return ClientboundRemoveMobEffectPacket.class;
    }
}
