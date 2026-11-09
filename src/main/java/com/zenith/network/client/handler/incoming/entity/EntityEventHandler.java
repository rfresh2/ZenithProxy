package com.zenith.network.client.handler.incoming.entity;

import com.zenith.Proxy;
import com.zenith.event.module.TotemPopEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EntityEvent;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundEntityEventPacket;

import static com.zenith.Globals.*;

public class EntityEventHandler implements ClientEventLoopPacketHandler<ClientboundEntityEventPacket, ClientSession> {
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
        if (!Proxy.getInstance().hasActivePlayer() && packet.getEntityId() == CACHE.getPlayerCache().getEntityId()) {
            BOT.onEntityEvent(packet.getEvent());
        }
        return true;
    }
}
