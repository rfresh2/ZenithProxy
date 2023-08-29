package com.zenith.cache.data.stats;

import com.github.steveice10.mc.protocol.data.game.advancement.Advancement;
import com.github.steveice10.mc.protocol.data.game.recipe.Recipe;
import com.github.steveice10.mc.protocol.data.game.statistic.Statistic;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundUpdateAdvancementsPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundUpdateRecipesPacket;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.cache.CachedData;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


@Getter
@Setter
@Accessors(chain = true)
public class StatisticsCache implements CachedData {
    protected final Map<Statistic, Integer> statistics = Collections.synchronizedMap(new Object2IntOpenHashMap<>());

    protected final List<Advancement> advancements = Collections.synchronizedList(new ArrayList<>());
    protected final Map<String, Map<String, Long>> progress = Collections.synchronizedMap(new Object2ObjectOpenHashMap<>());

    protected List<Recipe> recipes = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer) {
        consumer.accept(new ClientboundUpdateAdvancementsPacket(
                true,
                this.advancements.toArray(new Advancement[0]),
                new String[0],
                this.progress
        ));
        consumer.accept(new ClientboundUpdateRecipesPacket(
                this.recipes.toArray(new Recipe[0])
        ));
    }

    @Override
    public void reset(boolean full) {
        this.statistics.clear();
        this.advancements.clear();
        this.progress.clear();
        this.recipes.clear();
    }
}
