package com.zenith.network.client.handler.incoming.scoreboard;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetDisplayObjectivePacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;

public class SetDisplayObjectiveHandler implements AsyncPacketHandler<ClientboundSetDisplayObjectivePacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundSetDisplayObjectivePacket packet, @NonNull ClientSession session) {
        CACHE.getScoreboardCache().setPositionObjective(packet.getPosition(), packet.getName());
        return true;
    }
}
