package com.zenith.network.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundUpdateMobEffectPacket;
import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityLiving;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.cache.data.entity.PotionEffect;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CLIENT_LOG;

public class UpdateMobEffectHandler implements AsyncIncomingHandler<ClientboundUpdateMobEffectPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundUpdateMobEffectPacket packet, @NonNull ClientSession session) {
        try {
            Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
            if (entity != null) {
                if (entity instanceof EntityLiving) {
                    // todo: lol this class inheritance structure needs some rethinking
                    ((EntityLiving) entity).getPotionEffectMap().put(packet.getEffect(), new PotionEffect(
                        packet.getEffect(),
                        packet.getAmplifier(),
                        packet.getDuration(),
                        packet.isAmbient(),
                        packet.isShowParticles(),
                        packet.getFactorData()
                    ));
                } else if (entity instanceof EntityPlayer) {
                    ((EntityPlayer) entity).getPotionEffectMap().put(packet.getEffect(), new PotionEffect(
                        packet.getEffect(),
                        packet.getAmplifier(),
                        packet.getDuration(),
                        packet.isAmbient(),
                        packet.isShowParticles(),
                        packet.getFactorData()
                    ));
                }
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
    public Class<ClientboundUpdateMobEffectPacket> getPacketClass() {
        return ClientboundUpdateMobEffectPacket.class;
    }
}
