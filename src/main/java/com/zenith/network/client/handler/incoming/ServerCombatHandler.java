package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerCombatKillPacket;
import com.zenith.event.proxy.DeathEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.EVENT_BUS;

public class ServerCombatHandler implements AsyncIncomingHandler<ClientboundPlayerCombatKillPacket, ClientSession> {

    @Override
    public boolean applyAsync(ClientboundPlayerCombatKillPacket packet, ClientSession session) {
        if (packet.getPlayerId() == CACHE.getPlayerCache().getEntityId()) {
            EVENT_BUS.postAsync(new DeathEvent());
        }
        return true;
    }

    @Override
    public Class<ClientboundPlayerCombatKillPacket> getPacketClass() {
        return ClientboundPlayerCombatKillPacket.class;
    }
}
