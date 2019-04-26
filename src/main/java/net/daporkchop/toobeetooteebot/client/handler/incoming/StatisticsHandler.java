package net.daporkchop.toobeetooteebot.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerStatisticsPacket;
import lombok.NonNull;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.daporkchop.toobeetooteebot.client.PorkClientSession;
import net.daporkchop.toobeetooteebot.util.handler.HandlerRegistry;

import java.util.HashMap;

/**
 * @author DaPorkchop_
 */
public class StatisticsHandler implements HandlerRegistry.IncomingHandler<ServerStatisticsPacket, PorkClientSession> {
    protected static final long PACKET_STATISTICS_OFFSET = PUnsafe.pork_getOffset(ServerStatisticsPacket.class, "statistics");

    @Override
    public boolean apply(@NonNull ServerStatisticsPacket packet, @NonNull PorkClientSession session) {
        CACHE.getStatsCache().getStatistics().putAll(packet.getStatistics()); //cache all locally
        PUnsafe.putObject(packet, PACKET_STATISTICS_OFFSET, new HashMap<>(CACHE.getStatsCache().getStatistics())); //replace statistics packet with copy of local
        return true;
    }

    @Override
    public Class<ServerStatisticsPacket> getPacketClass() {
        return ServerStatisticsPacket.class;
    }
}
