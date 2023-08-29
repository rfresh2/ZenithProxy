package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundBlockUpdatePacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;

public class BlockChangeHandler implements AsyncIncomingHandler<ClientboundBlockUpdatePacket, ClientSession> {

    @Override
    public boolean applyAsync(@NonNull ClientboundBlockUpdatePacket packet, @NonNull ClientSession session) {
        return CACHE.getChunkCache().updateBlock(packet);
    }

    @Override
    public Class<ClientboundBlockUpdatePacket> getPacketClass() {
        return ClientboundBlockUpdatePacket.class;
    }
}
