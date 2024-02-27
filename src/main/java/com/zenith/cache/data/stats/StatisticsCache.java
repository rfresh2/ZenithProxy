package com.zenith.cache.data.stats;

import com.github.steveice10.mc.protocol.data.game.advancement.Advancement;
import com.github.steveice10.mc.protocol.data.game.statistic.Statistic;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundUpdateAdvancementsPacket;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.cache.CachedData;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


@Data
@Accessors(chain = true)
public class StatisticsCache implements CachedData {
    protected final Map<Statistic, Integer> statistics = new ConcurrentHashMap<>();

    protected final List<Advancement> advancements = Collections.synchronizedList(new ArrayList<>());
    protected final Map<String, Map<String, Long>> progress = new ConcurrentHashMap<>();


    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer) {
        consumer.accept(new ClientboundUpdateAdvancementsPacket(
                true,
                this.advancements.toArray(new Advancement[0]),
                new String[0],
                this.progress
        ));
    }

    @Override
    public void reset(boolean full) {
        this.statistics.clear();
        this.advancements.clear();
        this.progress.clear();
    }
}
