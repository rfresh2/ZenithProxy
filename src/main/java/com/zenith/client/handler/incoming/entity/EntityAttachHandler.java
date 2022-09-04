package com.zenith.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityAttachPacket;
import com.zenith.cache.data.entity.Entity;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;

import static com.zenith.util.Constants.CACHE;
import static com.zenith.util.Constants.CLIENT_LOG;

public class EntityAttachHandler implements HandlerRegistry.AsyncIncomingHandler<ServerEntityAttachPacket, ClientSession> {

    @Override
    public boolean applyAsync(ServerEntityAttachPacket packet, ClientSession session) {
        Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
        if (entity != null) {
            if (packet.getAttachedToId() == -1) {
                entity.setLeashed(false).setLeashedId(-1);
            } else {
                entity.setLeashed(true).setLeashedId(packet.getAttachedToId());
            }
            return true;
        } else {
            CLIENT_LOG.warn("Received ServerEntityAttachPacket for invalid entity (id=%d)", packet.getEntityId());
            return false;
        }
    }

    @Override
    public Class<ServerEntityAttachPacket> getPacketClass() {
        return ServerEntityAttachPacket.class;
    }
}
