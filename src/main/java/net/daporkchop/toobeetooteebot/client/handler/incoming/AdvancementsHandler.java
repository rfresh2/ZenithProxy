package net.daporkchop.toobeetooteebot.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerAdvancementsPacket;
import lombok.NonNull;
import net.daporkchop.toobeetooteebot.client.PorkClientSession;
import net.daporkchop.toobeetooteebot.util.handler.HandlerRegistry;

import java.util.HashMap;

/**
 * @author DaPorkchop_
 */
public class AdvancementsHandler implements HandlerRegistry.IncomingHandler<ServerAdvancementsPacket, PorkClientSession> {
    @Override
    public boolean apply(@NonNull ServerAdvancementsPacket packet, @NonNull PorkClientSession session) {
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
