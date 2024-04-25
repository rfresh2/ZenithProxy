package com.zenith.network.client.handler.incoming.entity;

import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityLiving;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRemoveMobEffectPacket;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CLIENT_LOG;

public class RemoveMobEffectHandler implements ClientEventLoopPacketHandler<ClientboundRemoveMobEffectPacket, ClientSession> {

    @Override
    public boolean applyAsync(ClientboundRemoveMobEffectPacket packet, ClientSession session) {
        try {
            Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
            if (entity instanceof EntityLiving e) {
                e.getPotionEffectMap().remove(packet.getEffect());
            } else {
                CLIENT_LOG.debug("Received ClientboundRemoveMobEffectPacket for invalid entity (id={})", packet.getEntityId());
                return false;
            }
        } catch (Exception e) {
            CLIENT_LOG.debug("Failed handling ClientboundRemoveMobEffectPacket for entity (id={})", packet.getEntityId(), e);
        }
        return true;
    }
}
