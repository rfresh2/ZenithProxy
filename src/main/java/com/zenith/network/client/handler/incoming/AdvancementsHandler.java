package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerAdvancementsPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import java.util.HashMap;

import static com.zenith.Shared.CACHE;

public class AdvancementsHandler implements AsyncIncomingHandler<ServerAdvancementsPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerAdvancementsPacket packet, @NonNull ClientSession session) {
        if (packet.doesReset()) {
            CACHE.getStatsCache().getAdvancements().clear();
            CACHE.getStatsCache().getProgress().clear();
        }
        CACHE.getStatsCache().getAdvancements().addAll(packet.getAdvancements());
        CACHE.getStatsCache().getAdvancements().removeIf(advancement -> packet.getRemovedAdvancements().contains(advancement.getId()));
        packet.getProgress().forEach((id, criterions) -> CACHE.getStatsCache().getProgress().computeIfAbsent(id, s -> new HashMap<>()).putAll(criterions));
        return true;
    }

    @Override
    public Class<ServerAdvancementsPacket> getPacketClass() {
        return ServerAdvancementsPacket.class;
    }
}
