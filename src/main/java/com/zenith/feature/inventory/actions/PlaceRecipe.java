package com.zenith.feature.inventory.actions;

import com.zenith.mc.item.ItemData;
import com.zenith.mc.item.ItemRegistry;
import lombok.Data;
import net.kyori.adventure.key.Key;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.ShapedCraftingRecipeDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.ShapelessCraftingRecipeDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.ItemSlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.ItemStackSlotDisplay;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundPlaceRecipePacket;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.CLIENT_LOG;

/**
 * Warning: unstable interface
 * May change in future MC versions
 */
@Data
@ApiStatus.Experimental
public class PlaceRecipe implements InventoryAction {
    private final int containerId;
    private final int recipeId;
    private final boolean useMaxItems;

    public PlaceRecipe(int containerId, int recipeId, boolean useMaxItems) {
        this.containerId = containerId;
        this.recipeId = recipeId;
        this.useMaxItems = useMaxItems;
    }

    /**
     * Note: there can be multiple recipes with the same output item,
     * in which case this will choose a pseudo-random one.
     *
     * If you need more control over the exact recipe, use {@link #PlaceRecipe(int, int, boolean)} and
     * find the recipe ID you want manually from CACHE.getRecipeCache().getRecipeBookEntries()
     * it may be simpler to just use ClickItem and ShiftClick actions if you know the items and shape
     */
    @Deprecated
    public PlaceRecipe(int containerId, String recipeOutputItem, boolean useMaxItems) {
        this(containerId, findRecipeByOutputItem(recipeOutputItem), useMaxItems);
    }

    @Override
    public int containerId() {
        return containerId;
    }

    @Override
    public @Nullable MinecraftPacket packet() {
        if (!CACHE.getRecipeCache().getRecipeBookEntries().containsKey(recipeId)) {
            CLIENT_LOG.debug("No matching recipe found {}", this);
            return null;
        }
        return new ServerboundPlaceRecipePacket(containerId, recipeId, useMaxItems);
    }

    private static int findRecipeByOutputItem(String outputItemName) {
        String recipeKey;
        try {
            recipeKey = Key.key(outputItemName).value();
        } catch (Exception e) {
            CLIENT_LOG.debug("Invalid recipe key: {}", outputItemName, e);
            return -1;
        }
        ItemData itemData = ItemRegistry.REGISTRY.get(recipeKey);
        if (itemData == null) {
            CLIENT_LOG.debug("No item data found for recipe output item: {}", outputItemName);
            return -1;
        }
        for (var recipeBookEntry : CACHE.getRecipeCache().getRecipeBookEntries().int2ObjectEntrySet()) {
            if (!(recipeBookEntry.getValue().display() instanceof ShapedCraftingRecipeDisplay || recipeBookEntry.getValue().display() instanceof ShapelessCraftingRecipeDisplay)) {
                continue;
            }
            var displayResult = recipeBookEntry.getValue().display().result();
            switch (displayResult) {
                case ItemStackSlotDisplay(ItemStack itemStack) -> {
                    if (itemStack != null && itemStack.getId() == itemData.id()) {
                        return recipeBookEntry.getIntKey();
                    }
                }
                case ItemSlotDisplay(int itemId) -> {
                    if (itemId == itemData.id()) {
                        return recipeBookEntry.getIntKey();
                    }
                }
                default -> {}
            }
        }
        return -1;
    }
}
