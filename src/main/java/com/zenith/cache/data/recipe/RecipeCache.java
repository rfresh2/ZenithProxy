package com.zenith.cache.data.recipe;

import com.zenith.cache.CacheResetType;
import com.zenith.cache.CachedData;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Data;
import lombok.experimental.Accessors;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.tcp.TcpSession;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.CraftingBookStateType;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.RecipeDisplayEntry;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRecipeBookAddPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRecipeBookRemovePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRecipeBookSettingsPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundUpdateRecipesPacket;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Data
@Accessors(chain = true)
public class RecipeCache implements CachedData {
    protected Map<String, int[]> itemSets = new ConcurrentHashMap<>();
    protected List<ClientboundUpdateRecipesPacket.SelectableRecipe> stoneCutterRecipes = Collections.synchronizedList(new ArrayList<>());
    protected Map<CraftingBookStateType, ClientboundRecipeBookSettingsPacket.TypeSettings> recipeBookSettings = Collections.synchronizedMap(new EnumMap<>(CraftingBookStateType.class));
    protected Int2ObjectMap<RecipeDisplayEntry> recipeBookEntries = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>());
    static final ClientboundRecipeBookSettingsPacket.TypeSettings DEFAULT_TYPESETTINGS = new ClientboundRecipeBookSettingsPacket.TypeSettings(false, false);

    @Override
    public synchronized void getPackets(@NonNull final Consumer<Packet> consumer, final @NonNull TcpSession session) {
        consumer.accept(new ClientboundUpdateRecipesPacket(itemSets, new ArrayList<>(stoneCutterRecipes)));
        consumer.accept(new ClientboundRecipeBookSettingsPacket(
            recipeBookSettings.getOrDefault(CraftingBookStateType.CRAFTING, DEFAULT_TYPESETTINGS),
            recipeBookSettings.getOrDefault(CraftingBookStateType.FURNACE, DEFAULT_TYPESETTINGS),
            recipeBookSettings.getOrDefault(CraftingBookStateType.BLAST_FURNACE, DEFAULT_TYPESETTINGS),
            recipeBookSettings.getOrDefault(CraftingBookStateType.SMOKER, DEFAULT_TYPESETTINGS)
        ));
        final List<ClientboundRecipeBookAddPacket.Entry> entries = new ArrayList<>(recipeBookEntries.size());
        for (var entry : recipeBookEntries.int2ObjectEntrySet()) {
            entries.add(new ClientboundRecipeBookAddPacket.Entry(entry.getValue(), false, false));
        }
        consumer.accept(new ClientboundRecipeBookAddPacket(entries, true));
    }

    @Override
    public synchronized void reset(CacheResetType type) {
        if (type == CacheResetType.FULL || type == CacheResetType.PROTOCOL_SWITCH) {
            this.itemSets.clear();
            this.stoneCutterRecipes.clear();
            this.recipeBookSettings.clear();
            this.recipeBookEntries.clear();
        }
    }

    public synchronized void setRecipeRegistry(final ClientboundUpdateRecipesPacket packet) {
        this.itemSets.clear();
        this.stoneCutterRecipes.clear();
        this.itemSets.putAll(packet.getItemSets());
        this.stoneCutterRecipes.addAll(packet.getStonecutterRecipes());
    }

    public void addRecipeBookEntries(final ClientboundRecipeBookAddPacket packet) {
        if (packet.isReplace()) {
            this.recipeBookEntries.clear();
        }
        for (var entry : packet.getEntries()) {
            int key = entry.contents().id();
            this.recipeBookEntries.put(key, entry.contents());
        }
    }

    public void setRecipeBookSettings(final ClientboundRecipeBookSettingsPacket packet) {
        this.recipeBookSettings.put(CraftingBookStateType.CRAFTING, packet.getCrafting());
        this.recipeBookSettings.put(CraftingBookStateType.FURNACE, packet.getFurnace());
        this.recipeBookSettings.put(CraftingBookStateType.BLAST_FURNACE, packet.getBlastFurnace());
        this.recipeBookSettings.put(CraftingBookStateType.SMOKER, packet.getSmoker());
    }

    public void removeRecipeBookEntries(final ClientboundRecipeBookRemovePacket packet) {
        for (var id : packet.getRecipes()) {
            this.recipeBookEntries.remove(id);
        }
    }

    @Override
    public String getSendingMessage()  {
        return String.format("Sending %d recipes", this.itemSets.size());
    }

}
