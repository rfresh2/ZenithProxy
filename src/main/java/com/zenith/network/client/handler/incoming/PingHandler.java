package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPingPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundPongPacket;
import com.zenith.Proxy;
import com.zenith.module.impl.PlayerSimulation;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.IncomingHandler;

import static com.zenith.Shared.MODULE_MANAGER;

public class PingHandler implements IncomingHandler<ClientboundPingPacket, ClientSession> {
    @Override
    public boolean apply(final ClientboundPingPacket packet, final ClientSession session) {
        // grim ac uses this to determine leniency in player movements. should be synced to actual ping from player
        if (Proxy.getInstance().getCurrentPlayer().get() == null) {
            MODULE_MANAGER.get(PlayerSimulation.class).addTask(() -> session.send(new ServerboundPongPacket(packet.getId())));
        }
        return true;
    }

    @Override
    public Class<ClientboundPingPacket> getPacketClass() {
        return ClientboundPingPacket.class;
    }
}
