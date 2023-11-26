package com.zenith.network.client.handler.incoming.level;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundLightUpdatePacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;

import static com.zenith.Shared.CACHE;

public class LightUpdateHandler implements AsyncPacketHandler<ClientboundLightUpdatePacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundLightUpdatePacket packet, final ClientSession session) {
        return CACHE.getChunkCache().handleLightUpdate(packet);
    }
}
