package com.zenith.network.client.handler.incoming;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import lombok.NonNull;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundUpdateAdvancementsPacket;

import java.util.HashMap;
import java.util.List;

import static com.zenith.Shared.CACHE;

public class UpdateAdvancementsHandler implements ClientEventLoopPacketHandler<ClientboundUpdateAdvancementsPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundUpdateAdvancementsPacket packet, @NonNull ClientSession session) {
        if (packet.isReset()) {
            CACHE.getStatsCache().getAdvancements().clear();
            CACHE.getStatsCache().getProgress().clear();
        }
        CACHE.getStatsCache().getAdvancements().addAll(List.of(packet.getAdvancements()));
        CACHE.getStatsCache().getAdvancements().removeIf(advancement -> List.of(packet.getRemovedAdvancements()).contains(advancement.getId()));
        packet.getProgress().forEach((id, criterions) -> CACHE.getStatsCache().getProgress().computeIfAbsent(id, s -> new HashMap<>()).putAll(criterions));
        return true;
    }
}
