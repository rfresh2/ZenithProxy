package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundAwardStatsPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;

public class AwardStatsHandler implements ClientEventLoopPacketHandler<ClientboundAwardStatsPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundAwardStatsPacket packet, @NonNull ClientSession session) {
        CACHE.getStatsCache().getStatistics().putAll(packet.getStatistics()); //cache all locally
        return true;
    }
}
