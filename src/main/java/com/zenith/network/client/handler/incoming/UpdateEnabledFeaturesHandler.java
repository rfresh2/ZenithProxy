package com.zenith.network.client.handler.incoming;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundUpdateEnabledFeaturesPacket;

import static com.zenith.Shared.CACHE;

public class UpdateEnabledFeaturesHandler implements ClientEventLoopPacketHandler<ClientboundUpdateEnabledFeaturesPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundUpdateEnabledFeaturesPacket packet, final ClientSession session) {
        CACHE.getConfigurationCache().setEnabledFeatures(packet.getFeatures());
        return true;
    }
}
