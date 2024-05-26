package com.zenith.cache.data.stats;

import com.zenith.cache.CachedData;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.advancement.Advancement;
import org.geysermc.mcprotocollib.protocol.data.game.statistic.Statistic;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundAwardStatsPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundUpdateAdvancementsPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


@Data
@Accessors(chain = true)
public class StatisticsCache implements CachedData {
    protected final Object2IntMap<Statistic> statistics = new Object2IntOpenHashMap<>();

    protected final List<Advancement> advancements = Collections.synchronizedList(new ArrayList<>());
    protected final Map<String, Map<String, Long>> progress = new ConcurrentHashMap<>();


    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer) {
        consumer.accept(new ClientboundUpdateAdvancementsPacket(
                true,
                this.advancements.toArray(Advancement[]::new),
                new String[0],
                this.progress
        ));
        // avoiding possible concurrent modification
        Object2IntMap<Statistic> stats = new Object2IntOpenHashMap<>(statistics.size());
        stats.putAll(statistics);
        consumer.accept(new ClientboundAwardStatsPacket(stats));
    }

    @Override
    public void reset(boolean full) {
        this.statistics.clear();
        this.advancements.clear();
        this.progress.clear();
    }
}
