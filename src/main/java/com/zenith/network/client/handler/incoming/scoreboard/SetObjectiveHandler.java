package com.zenith.network.client.handler.incoming.scoreboard;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetObjectivePacket;
import com.zenith.cache.data.scoreboard.Objective;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;

public class SetObjectiveHandler implements AsyncPacketHandler<ClientboundSetObjectivePacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundSetObjectivePacket packet, @NonNull ClientSession session) {
        switch (packet.getAction()) {
            case ADD -> CACHE.getScoreboardCache().add(packet);
            case REMOVE -> CACHE.getScoreboardCache().remove(packet);
            case UPDATE -> {
                final Objective objective = CACHE.getScoreboardCache().get(packet.getName());
                if (objective == null) {
                    return false;
                }
                objective
                    .setDisplayName(packet.getDisplayName())
                    .setScoreType(packet.getType());
            }
        }

        return true;
    }
}
