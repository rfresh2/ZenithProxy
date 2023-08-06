package com.zenith.network.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityRemoveEffectPacket;
import com.zenith.cache.data.entity.EntityEquipment;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CLIENT_LOG;
import static java.util.Objects.nonNull;

public class EntityRemoveEffectHandler implements AsyncIncomingHandler<ServerEntityRemoveEffectPacket, ClientSession> {

    @Override
    public boolean applyAsync(ServerEntityRemoveEffectPacket packet, ClientSession session) {
        try {
            EntityEquipment entity = CACHE.getEntityCache().get(packet.getEntityId());
            if (nonNull(entity)) {
                try {
                    entity.getPotionEffectMap().remove(packet.getEffect());
                } catch (final Exception e) {
                    CLIENT_LOG.warn("Failed removing entity effects", e);
                    return false;
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
    public Class<ServerEntityRemoveEffectPacket> getPacketClass() {
        return ServerEntityRemoveEffectPacket.class;
    }
}
