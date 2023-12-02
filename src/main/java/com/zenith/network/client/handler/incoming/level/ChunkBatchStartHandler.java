package com.zenith.network.client.handler.incoming.level;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundChunkBatchStartPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;

public class ChunkBatchStartHandler implements AsyncPacketHandler<ClientboundChunkBatchStartPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundChunkBatchStartPacket packet, final ClientSession session) {
        // do we really care about rate limiting chunks?
        return true;
    }
}
