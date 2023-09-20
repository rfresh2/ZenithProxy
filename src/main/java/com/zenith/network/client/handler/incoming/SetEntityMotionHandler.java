package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityMotionPacket;
import com.zenith.Proxy;
import com.zenith.module.impl.PlayerSimulation;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.IncomingHandler;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.MODULE_MANAGER;

public class SetEntityMotionHandler implements IncomingHandler<ClientboundSetEntityMotionPacket, ClientSession> {
    @Override
    public boolean apply(final ClientboundSetEntityMotionPacket packet, final ClientSession session) {
        if (Proxy.getInstance().getCurrentPlayer().get() == null && packet.getEntityId() == CACHE.getPlayerCache().getEntityId()) {
            MODULE_MANAGER.get(PlayerSimulation.class).handleSetMotion(packet.getMotionX(), packet.getMotionY(), packet.getMotionZ());
        }
        return true;
    }

    @Override
    public Class<ClientboundSetEntityMotionPacket> getPacketClass() {
        return ClientboundSetEntityMotionPacket.class;
    }
}
