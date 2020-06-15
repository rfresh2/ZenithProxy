/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2016-2020 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.toobeetooteebot.util.cache.data.stats;

import com.github.steveice10.mc.protocol.data.game.advancement.Advancement;
import com.github.steveice10.mc.protocol.data.game.statistic.Statistic;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerAdvancementsPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerUnlockRecipesPacket;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.daporkchop.toobeetooteebot.util.cache.CachedData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * @author DaPorkchop_
 */
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
        if (full)   {
            this.statistics.clear();

            this.advancements.clear();
            this.progress.clear();

            this.openCraftingBook = this.activateFiltering = false;
            this.recipes.clear();
            this.alreadyKnownRecipes.clear();
        }
    }
}
