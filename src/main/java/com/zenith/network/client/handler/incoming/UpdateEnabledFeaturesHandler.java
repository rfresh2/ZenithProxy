package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundUpdateEnabledFeaturesPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;

import static com.zenith.Shared.CACHE;

public class UpdateEnabledFeaturesHandler implements ClientEventLoopPacketHandler<ClientboundUpdateEnabledFeaturesPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundUpdateEnabledFeaturesPacket packet, final ClientSession session) {
        CACHE.getConfigurationCache().setEnabledFeatures(packet.getFeatures());
        return true;
    }
}
