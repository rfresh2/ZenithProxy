package com.zenith.network.client.handler.incoming.level;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSectionBlocksUpdatePacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;

public class SectionBlocksUpdateHandler implements AsyncPacketHandler<ClientboundSectionBlocksUpdatePacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundSectionBlocksUpdatePacket packet, @NonNull ClientSession session) {
        return CACHE.getChunkCache().multiBlockUpdate(packet);
    }
}
