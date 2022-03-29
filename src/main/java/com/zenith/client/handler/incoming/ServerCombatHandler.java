package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.data.game.entity.player.CombatState;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerCombatPacket;
import com.zenith.client.PorkClientSession;
import com.zenith.event.proxy.DeathEvent;
import com.zenith.util.handler.HandlerRegistry;

import static com.zenith.util.Constants.CACHE;
import static com.zenith.util.Constants.EVENT_BUS;

public class ServerCombatHandler implements HandlerRegistry.IncomingHandler<ServerCombatPacket, PorkClientSession> {

    @Override
    public boolean apply(ServerCombatPacket packet, PorkClientSession session) {
        if (packet.getPlayerId() == CACHE.getPlayerCache().getEntityId()) {
            if (packet.getCombatState() == CombatState.ENTITY_DEAD) {
                EVENT_BUS.dispatch(new DeathEvent(packet.getMessage()));
            }
        }
        return true;
    }

    @Override
    public Class<ServerCombatPacket> getPacketClass() {
        return ServerCombatPacket.class;
    }
}
