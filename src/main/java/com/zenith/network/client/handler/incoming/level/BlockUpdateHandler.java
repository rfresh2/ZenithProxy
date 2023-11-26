package com.zenith.network.client.handler.incoming.level;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundBlockUpdatePacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;

public class BlockUpdateHandler implements AsyncPacketHandler<ClientboundBlockUpdatePacket, ClientSession> {

    @Override
    public boolean applyAsync(@NonNull ClientboundBlockUpdatePacket packet, @NonNull ClientSession session) {
        return CACHE.getChunkCache().updateBlock(packet);
    }
}
