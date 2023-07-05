package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.data.game.entity.player.CombatState;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerCombatPacket;
import com.zenith.client.ClientSession;
import com.zenith.event.proxy.DeathEvent;
import com.zenith.feature.handler.HandlerRegistry;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.EVENT_BUS;

public class ServerCombatHandler implements HandlerRegistry.AsyncIncomingHandler<ServerCombatPacket, ClientSession> {

    @Override
    public boolean applyAsync(ServerCombatPacket packet, ClientSession session) {
        if (packet.getPlayerId() == CACHE.getPlayerCache().getEntityId()) {
            if (packet.getCombatState() == CombatState.ENTITY_DEAD) {
                // packet.message() value is basically garbage on 2b2t, same message on any death, might have more info on other servers
                // see ChatHandler for how we try to grab the actual informative death message
                EVENT_BUS.dispatch(new DeathEvent());
            }
        }
        return true;
    }

    @Override
    public Class<ServerCombatPacket> getPacketClass() {
        return ServerCombatPacket.class;
    }
}
