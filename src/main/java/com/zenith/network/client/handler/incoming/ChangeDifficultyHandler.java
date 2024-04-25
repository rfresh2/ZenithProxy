package com.zenith.network.client.handler.incoming;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundChangeDifficultyPacket;

import static com.zenith.Shared.CACHE;

public class ChangeDifficultyHandler implements ClientEventLoopPacketHandler<ClientboundChangeDifficultyPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundChangeDifficultyPacket packet, final ClientSession session) {
        CACHE.getPlayerCache().setDifficulty(packet.getDifficulty());
        CACHE.getPlayerCache().setDifficultyLocked(packet.isDifficultyLocked());
        return true;
    }
}
