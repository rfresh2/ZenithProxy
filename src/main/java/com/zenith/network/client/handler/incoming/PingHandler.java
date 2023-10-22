package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPingPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundPongPacket;
import com.zenith.Proxy;
import com.zenith.module.impl.PlayerSimulation;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;

import static com.zenith.Shared.MODULE_MANAGER;

public class PingHandler implements AsyncIncomingHandler<ClientboundPingPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundPingPacket packet, final ClientSession session) {
        // grim ac uses this to determine leniency in player movements. should be synced to actual ping from player
        if (Proxy.getInstance().getCurrentPlayer().get() == null) {
            MODULE_MANAGER.get(PlayerSimulation.class).addTask(() -> session.sendAsync(new ServerboundPongPacket(packet.getId())));
        }
        return true;
    }
}
