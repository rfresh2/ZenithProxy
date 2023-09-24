package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundUpdateEnabledFeaturesPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;

import static com.zenith.Shared.CACHE;

public class UpdateEnabledFeaturesHandler implements AsyncIncomingHandler<ClientboundUpdateEnabledFeaturesPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundUpdateEnabledFeaturesPacket packet, final ClientSession session) {
        CACHE.getPlayerCache().setEnabledFeatures(packet.getFeatures());
        return true;
    }
}
