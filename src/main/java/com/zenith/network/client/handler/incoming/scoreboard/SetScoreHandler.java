package com.zenith.network.client.handler.incoming.scoreboard;

import com.zenith.cache.data.scoreboard.Score;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import lombok.NonNull;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetScorePacket;

import static com.zenith.Shared.CACHE;

public class SetScoreHandler implements ClientEventLoopPacketHandler<ClientboundSetScorePacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundSetScorePacket packet, @NonNull ClientSession session) {
        var objective = CACHE.getScoreboardCache().get(packet.getObjective());
        if (objective == null) return false;

        objective.getScores().put(packet.getOwner(), new Score(packet));
        return true;
    }
}
