package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundUpdateTagsPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;

import static com.zenith.Shared.CACHE;

public class UpdateTagsHandler implements AsyncIncomingHandler<ClientboundUpdateTagsPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundUpdateTagsPacket packet, final ClientSession session) {
        CACHE.getPlayerCache().setTags(packet.getTags());
        return true;
    }
}
