package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerStatisticsPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;

public class StatisticsHandler implements AsyncIncomingHandler<ServerStatisticsPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerStatisticsPacket packet, @NonNull ClientSession session) {
        CACHE.getStatsCache().getStatistics().putAll(packet.getStatistics()); //cache all locally
        return true;
    }

    @Override
    public Class<ServerStatisticsPacket> getPacketClass() {
        return ServerStatisticsPacket.class;
    }
}
