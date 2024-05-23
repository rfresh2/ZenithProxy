package com.zenith.cache.data.recipe;

import com.zenith.cache.CachedData;
import lombok.Data;
import lombok.NonNull;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.Recipe;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRecipePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundUpdateRecipesPacket;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Arrays.asList;

@Data
public class RecipeCache implements CachedData {
    protected Set<Recipe> recipeRegistry = Collections.synchronizedSet(new HashSet<>());
    // todo: still some issues with known/displayed to debug
    protected Set<String> knownRecipes = Collections.synchronizedSet(new HashSet<>());
    protected Set<String> displayedRecipes = Collections.synchronizedSet(new HashSet<>());
    private boolean openCraftingBook;
    private boolean activateCraftingFiltering;
    private boolean openSmeltingBook;
    private boolean activateSmeltingFiltering;
    private boolean openBlastingBook;
    private boolean activateBlastingFiltering;
    private boolean openSmokingBook;
    private boolean activateSmokingFiltering;

    @Override
    public synchronized void getPackets(@NonNull final Consumer<Packet> consumer) {
        consumer.accept(new ClientboundUpdateRecipesPacket(recipeRegistry.toArray(new Recipe[0])));
        // just unlock all recipes instead of using what's cached lol
        final String[] allRecipeIds = recipeRegistry.stream()
            .map(Recipe::getIdentifier)
            .toArray(String[]::new);
        consumer.accept(new ClientboundRecipePacket(
            allRecipeIds,
            openCraftingBook,
            activateCraftingFiltering,
            openSmeltingBook,
            activateSmeltingFiltering,
            openBlastingBook,
            activateBlastingFiltering,
            openSmokingBook,
            activateSmokingFiltering,
            allRecipeIds
        ));
    }

    @Override
    public synchronized void reset(final boolean full) {
        if (full) {
            this.recipeRegistry.clear();
            this.knownRecipes.clear();
            this.displayedRecipes.clear();
            this.openCraftingBook = false;
            this.activateCraftingFiltering = false;
            this.openSmeltingBook = false;
            this.activateSmeltingFiltering = false;
            this.openBlastingBook = false;
            this.activateBlastingFiltering = false;
            this.openSmokingBook = false;
            this.activateSmokingFiltering = false;
        }
    }

    public synchronized void setRecipeRegistry(final ClientboundUpdateRecipesPacket packet) {
        this.recipeRegistry.clear();
        this.recipeRegistry.addAll(List.of(packet.getRecipes()));
    }

    public synchronized void updateUnlockedRecipes(final ClientboundRecipePacket packet) {
        switch (packet.getAction()) {
            case INIT -> {
                this.knownRecipes.addAll(asList(packet.getRecipeIdsToChange()));
                this.displayedRecipes.addAll(asList(packet.getRecipeIdsToInit()));
            }
            case ADD -> {
                final List<String> toAdd = asList(packet.getRecipeIdsToChange());
                this.knownRecipes.addAll(toAdd);
                this.displayedRecipes.addAll(toAdd);
            }
            case REMOVE -> {
                final List<String> toRemove = asList(packet.getRecipeIdsToChange());
                toRemove.forEach(this.knownRecipes::remove);
                toRemove.forEach(this.displayedRecipes::remove);
            }
        }
        this.openCraftingBook = packet.isOpenCraftingBook();
        this.activateCraftingFiltering = packet.isActivateCraftingFiltering();
        this.openSmeltingBook = packet.isOpenSmeltingBook();
        this.activateSmeltingFiltering = packet.isActivateSmeltingFiltering();
        this.openBlastingBook = packet.isOpenBlastingBook();
        this.activateBlastingFiltering = packet.isActivateBlastingFiltering();
        this.openSmokingBook = packet.isOpenSmokingBook();
        this.activateSmokingFiltering = packet.isActivateSmokingFiltering();
    }
}
