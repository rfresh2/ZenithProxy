package com.zenith.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityRemoveEffectPacket;
import com.zenith.client.PorkClientSession;
import com.zenith.util.cache.data.entity.EntityEquipment;
import com.zenith.util.handler.HandlerRegistry;

import java.util.stream.Collectors;

import static com.zenith.util.Constants.CACHE;
import static com.zenith.util.Constants.CLIENT_LOG;
import static java.util.Objects.nonNull;

public class EntityRemoveEffectHandler implements HandlerRegistry.AsyncIncomingHandler<ServerEntityRemoveEffectPacket, PorkClientSession> {

    @Override
    public boolean applyAsync(ServerEntityRemoveEffectPacket packet, PorkClientSession session) {
        try {
            EntityEquipment entity = CACHE.getEntityCache().get(packet.getEntityId());
            if (nonNull(entity)) {
                entity.setPotionEffects(entity.getPotionEffects().stream()
                        .filter(entityPotionEffect -> !entityPotionEffect.getEffect().equals(packet.getEffect()))
                        .collect(Collectors.toList()));
            } else {
                CLIENT_LOG.warn("Received ServerEntityRemoveEffectPacket for invalid entity (id=%d)", packet.getEntityId());
                return false;
            }
        } catch (ClassCastException e) {
            CLIENT_LOG.warn("Received ServerEntityRemoveEffectPacket for non-equipment entity (id=%d)", e, packet.getEntityId());
        }
        return true;
    }

    @Override
    public Class<ServerEntityRemoveEffectPacket> getPacketClass() {
        return ServerEntityRemoveEffectPacket.class;
    }
}
