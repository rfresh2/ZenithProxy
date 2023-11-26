package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundChangeDifficultyPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;

import static com.zenith.Shared.CACHE;

public class ChangeDifficultyHandler implements AsyncPacketHandler<ClientboundChangeDifficultyPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundChangeDifficultyPacket packet, final ClientSession session) {
        CACHE.getPlayerCache().setDifficulty(packet.getDifficulty());
        CACHE.getPlayerCache().setDifficultyLocked(packet.isDifficultyLocked());
        return true;
    }
}
