package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundCustomPayloadPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;

import static com.zenith.Shared.CACHE;

public class CustomPayloadHandler implements AsyncIncomingHandler<ClientboundCustomPayloadPacket, ClientSession> {
    @Override
    public boolean applyAsync(ClientboundCustomPayloadPacket packet, ClientSession session) {
        if (packet.getChannel().equalsIgnoreCase("minecraft:brand"))
            CACHE.getChunkCache().setServerBrand(packet.getData());
        return true;
    }
}
