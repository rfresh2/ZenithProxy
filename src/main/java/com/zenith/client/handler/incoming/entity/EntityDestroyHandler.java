package com.zenith.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityDestroyPacket;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.util.Constants.CACHE;

public class EntityDestroyHandler implements HandlerRegistry.AsyncIncomingHandler<ServerEntityDestroyPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerEntityDestroyPacket packet, @NonNull ClientSession session) {
        for (int id : packet.getEntityIds()) {
            CACHE.getEntityCache().remove(id);
        }
        return true;
    }

    @Override
    public Class<ServerEntityDestroyPacket> getPacketClass() {
        return ServerEntityDestroyPacket.class;
    }
}
