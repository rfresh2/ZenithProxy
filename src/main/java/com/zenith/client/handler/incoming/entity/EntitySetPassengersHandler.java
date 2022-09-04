package com.zenith.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntitySetPassengersPacket;
import com.zenith.cache.data.entity.Entity;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import java.util.Arrays;
import java.util.stream.Collectors;

import static com.zenith.util.Constants.CACHE;
import static com.zenith.util.Constants.CLIENT_LOG;

public class EntitySetPassengersHandler implements HandlerRegistry.AsyncIncomingHandler<ServerEntitySetPassengersPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerEntitySetPassengersPacket packet, @NonNull ClientSession session) {
        Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
        if (entity != null) {
            entity.setPassengerIds(Arrays.stream(packet.getPassengerIds()).boxed().collect(Collectors.toList()));
            return true;
        } else {
            CLIENT_LOG.warn("Received ServerEntitySetPassengersPacket for invalid entity (id={})", packet.getEntityId());
            return false;
        }
    }

    @Override
    public Class<ServerEntitySetPassengersPacket> getPacketClass() {
        return ServerEntitySetPassengersPacket.class;
    }
}
