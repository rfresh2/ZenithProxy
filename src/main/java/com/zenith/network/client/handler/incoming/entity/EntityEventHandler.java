package com.zenith.network.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.data.game.entity.EntityEvent;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundEntityEventPacket;
import com.zenith.event.proxy.TotemPopEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.EVENT_BUS;

public class EntityEventHandler implements AsyncPacketHandler<ClientboundEntityEventPacket, ClientSession> {
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
        if (packet.getEvent() == EntityEvent.TOTEM_OF_UNDYING_MAKE_SOUND) {
            EVENT_BUS.postAsync(new TotemPopEvent(packet.getEntityId()));
        }
        return true;
    }
}
