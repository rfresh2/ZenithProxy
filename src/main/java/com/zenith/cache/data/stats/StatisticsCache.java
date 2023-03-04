package com.zenith.cache.data.stats;

import com.github.steveice10.mc.protocol.data.game.advancement.Advancement;
import com.github.steveice10.mc.protocol.data.game.statistic.Statistic;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerAdvancementsPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerUnlockRecipesPacket;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.cache.CachedData;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.*;
import java.util.function.Consumer;


@Getter
@Setter
@Accessors(chain = true)
public class StatisticsCache implements CachedData {
    protected final Map<Statistic, Integer> statistics = Collections.synchronizedMap(new HashMap<>());

    protected final List<Advancement> advancements = Collections.synchronizedList(new ArrayList<>());
    protected final Map<String, Map<String, Long>> progress = Collections.synchronizedMap(new HashMap<>());

    protected boolean openCraftingBook;
    protected boolean activateFiltering;
    protected final List<Integer> recipes = Collections.synchronizedList(new ArrayList<>());
    protected final List<Integer> alreadyKnownRecipes = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer) {
        consumer.accept(new ServerAdvancementsPacket(
                true,
                this.advancements,
                Collections.emptyList(),
                this.progress
        ));
        consumer.accept(new ServerUnlockRecipesPacket(
                this.openCraftingBook,
                this.activateFiltering,
                this.recipes,
                this.alreadyKnownRecipes
        ));
    }

    @Override
    public void reset(boolean full) {
        this.statistics.clear();

        this.advancements.clear();
        this.progress.clear();

        this.openCraftingBook = this.activateFiltering = false;
        this.recipes.clear();
        this.alreadyKnownRecipes.clear();
    }
}
