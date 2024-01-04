package com.zenith.network.client.handler.incoming.scoreboard;

import com.github.steveice10.mc.protocol.data.game.scoreboard.ScoreboardAction;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetScorePacket;
import com.zenith.cache.data.scoreboard.Objective;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;

public class SetScoreHandler implements AsyncPacketHandler<ClientboundSetScorePacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundSetScorePacket packet, @NonNull ClientSession session) {
        if (packet.getObjective().isEmpty() && packet.getAction() == ScoreboardAction.REMOVE) {
            CACHE.getScoreboardCache().removeEntry(packet.getEntry());
        } else {
            final Objective objective = CACHE.getScoreboardCache().get(packet.getObjective());
            if (objective == null) {
                return false;
            }

            switch (packet.getAction()) {
                case ADD_OR_UPDATE -> objective.getScores().put(packet.getEntry(), packet.getValue());
                case REMOVE -> objective.getScores().removeInt(packet.getEntry());
            }
        }
        return true;
    }
}
