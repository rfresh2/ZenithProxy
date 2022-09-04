package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerStatisticsPacket;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.HashMap;

import static com.zenith.util.Constants.CACHE;

public class StatisticsHandler implements HandlerRegistry.AsyncIncomingHandler<ServerStatisticsPacket, ClientSession> {
    protected static final long PACKET_STATISTICS_OFFSET = PUnsafe.pork_getOffset(ServerStatisticsPacket.class, "statistics");

    @Override
    public boolean applyAsync(@NonNull ServerStatisticsPacket packet, @NonNull ClientSession session) {
        CACHE.getStatsCache().getStatistics().putAll(packet.getStatistics()); //cache all locally
        PUnsafe.putObject(packet, PACKET_STATISTICS_OFFSET, new HashMap<>(CACHE.getStatsCache().getStatistics())); //replace statistics packet with copy of local
        return true;
    }

    @Override
    public Class<ServerStatisticsPacket> getPacketClass() {
        return ServerStatisticsPacket.class;
    }
}
