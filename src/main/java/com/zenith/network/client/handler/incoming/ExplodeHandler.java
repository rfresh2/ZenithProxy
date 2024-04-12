package com.zenith.network.client.handler.incoming;

import com.zenith.Proxy;
import com.zenith.module.impl.PlayerSimulation;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundExplodePacket;

import static com.zenith.Shared.MODULE;

public class ExplodeHandler implements ClientEventLoopPacketHandler<ClientboundExplodePacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundExplodePacket packet, final ClientSession session) {
        if (!Proxy.getInstance().hasActivePlayer())
            MODULE.get(PlayerSimulation.class).handleExplosion(packet);
        return true;
    }
}
