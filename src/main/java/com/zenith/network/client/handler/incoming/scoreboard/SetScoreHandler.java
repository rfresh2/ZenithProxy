package com.zenith.network.client.handler.incoming.scoreboard;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetScorePacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;

public class SetScoreHandler implements AsyncPacketHandler<ClientboundSetScorePacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundSetScorePacket packet, @NonNull ClientSession session) {
        switch (packet.getAction()) {
            case REMOVE -> CACHE.getScoreboardCache().removeEntry(packet.getEntry());
            case ADD_OR_UPDATE -> {
                var objective = CACHE.getScoreboardCache().get(packet.getObjective());
                if (objective == null) return false;
                objective.getScores().put(packet.getEntry(), packet.getValue());
            }
        }
        return true;
    }
}
