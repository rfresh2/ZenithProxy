package com.zenith.network.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetPassengersPacket;
import com.zenith.cache.data.entity.Entity;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CLIENT_LOG;

public class EntitySetPassengersHandler implements AsyncIncomingHandler<ClientboundSetPassengersPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundSetPassengersPacket packet, @NonNull ClientSession session) {
        Entity entity = CACHE.getEntityCache().get(packet.getEntityId());
        if (entity != null) {
            entity.setPassengerIds(IntArrayList.wrap(packet.getPassengerIds()));
            return true;
        } else {
            CLIENT_LOG.warn("Received ServerEntitySetPassengersPacket for invalid entity (id={})", packet.getEntityId());
            return false;
        }
    }

    @Override
    public Class<ClientboundSetPassengersPacket> getPacketClass() {
        return ClientboundSetPassengersPacket.class;
    }
}
