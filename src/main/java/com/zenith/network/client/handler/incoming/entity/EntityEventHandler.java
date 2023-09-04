package com.zenith.network.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.data.game.entity.EntityEvent;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundEntityEventPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;

import static com.zenith.Shared.CACHE;

public class EntityEventHandler implements AsyncIncomingHandler<ClientboundEntityEventPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundEntityEventPacket packet, final ClientSession session) {
        if (packet.getEntityId() == CACHE.getPlayerCache().getEntityId()) {
            if (packet.getEvent() == EntityEvent.PLAYER_OP_PERMISSION_LEVEL_0
                || packet.getEvent() == EntityEvent.PLAYER_OP_PERMISSION_LEVEL_1
                || packet.getEvent() == EntityEvent.PLAYER_OP_PERMISSION_LEVEL_2
                || packet.getEvent() == EntityEvent.PLAYER_OP_PERMISSION_LEVEL_3
                || packet.getEvent() == EntityEvent.PLAYER_OP_PERMISSION_LEVEL_4
            ) {
                CACHE.getPlayerCache().setOpLevel(packet.getEvent());
            }
        }
        return true;
    }

    @Override
    public Class<ClientboundEntityEventPacket> getPacketClass() {
        return ClientboundEntityEventPacket.class;
    }
}
