package com.zenith.cache.data.recipe;

import com.zenith.cache.CacheResetType;
import com.zenith.cache.CachedData;
import lombok.Data;
import lombok.NonNull;
import org.geysermc.mcprotocollib.network.packet.Packet;

import java.util.function.Consumer;

@Data
public class RecipeCache implements CachedData {
//    protected Set<Recipe> recipeRegistry = Collections.synchronizedSet(new ObjectOpenHashSet<>());
//    protected Set<String> knownRecipes = Collections.synchronizedSet(new ObjectOpenHashSet<>());
//    protected Set<String> displayedRecipes = Collections.synchronizedSet(new ObjectOpenHashSet<>());
//    private boolean openCraftingBook;
//    private boolean activateCraftingFiltering;
//    private boolean openSmeltingBook;
//    private boolean activateSmeltingFiltering;
//    private boolean openBlastingBook;
//    private boolean activateBlastingFiltering;
//    private boolean openSmokingBook;
//    private boolean activateSmokingFiltering;

    @Override
    public synchronized void getPackets(@NonNull final Consumer<Packet> consumer) {
//        consumer.accept(new ClientboundUpdateRecipesPacket(recipeRegistry.toArray(new Recipe[0])));
//        if (CONFIG.debug.server.cache.unlockAllRecipes) {
//            // technically bypassing the cache here isn't vanilla
//            // but it avoids any possible caching bugs and is useful for players
//            final String[] allRecipeIds = recipeRegistry.stream()
//                .map(Recipe::getIdentifier)
//                .toArray(String[]::new);
//            consumer.accept(new ClientboundRecipePacket(
//                allRecipeIds,
//                openCraftingBook,
//                activateCraftingFiltering,
//                openSmeltingBook,
//                activateSmeltingFiltering,
//                openBlastingBook,
//                activateBlastingFiltering,
//                openSmokingBook,
//                activateSmokingFiltering,
//                allRecipeIds
//            ));
//        } else {
//            consumer.accept(new ClientboundRecipePacket(
//                knownRecipes.toArray(String[]::new),
//                openCraftingBook,
//                activateCraftingFiltering,
//                openSmeltingBook,
//                activateSmeltingFiltering,
//                openBlastingBook,
//                activateBlastingFiltering,
//                openSmokingBook,
//                activateSmokingFiltering,
//                displayedRecipes.toArray(String[]::new)
//            ));
//        }
    }

    @Override
    public synchronized void reset(CacheResetType type) {
        if (type == CacheResetType.FULL || type == CacheResetType.PROTOCOL_SWITCH) {
//            this.recipeRegistry.clear();
//            this.knownRecipes.clear();
//            this.displayedRecipes.clear();
//            this.openCraftingBook = false;
//            this.activateCraftingFiltering = false;
//            this.openSmeltingBook = false;
//            this.activateSmeltingFiltering = false;
//            this.openBlastingBook = false;
//            this.activateBlastingFiltering = false;
//            this.openSmokingBook = false;
//            this.activateSmokingFiltering = false;
        }
    }

//    public synchronized void setRecipeRegistry(final ClientboundUpdateRecipesPacket packet) {
//        this.recipeRegistry.clear();
//        this.recipeRegistry.addAll(List.of(packet.getRecipes()));
//    }

//    public synchronized void updateUnlockedRecipes(final ClientboundRecipePacket packet) {
//        switch (packet.getAction()) {
//            case INIT -> {
//                for (int i = 0; i < packet.getRecipeIdsToChange().length; i++) {
//                    this.knownRecipes.add(packet.getRecipeIdsToChange()[i]);
//                }
//                for (int i = 0; i < packet.getRecipeIdsToInit().length; i++) {
//                    this.displayedRecipes.add(packet.getRecipeIdsToInit()[i]);
//                }
//            }
//            case ADD -> {
//                for (int i = 0; i < packet.getRecipeIdsToChange().length; i++) {
//                    var id = packet.getRecipeIdsToChange()[i];
//                    this.knownRecipes.add(id);
//                    this.displayedRecipes.add(id);
//                }
//            }
//            case REMOVE -> {
//                for (int i = 0; i < packet.getRecipeIdsToChange().length; i++) {
//                    this.displayedRecipes.remove(packet.getRecipeIdsToChange()[i]);
//                }
//            }
//        }
//        this.openCraftingBook = packet.isOpenCraftingBook();
//        this.activateCraftingFiltering = packet.isActivateCraftingFiltering();
//        this.openSmeltingBook = packet.isOpenSmeltingBook();
//        this.activateSmeltingFiltering = packet.isActivateSmeltingFiltering();
//        this.openBlastingBook = packet.isOpenBlastingBook();
//        this.activateBlastingFiltering = packet.isActivateBlastingFiltering();
//        this.openSmokingBook = packet.isOpenSmokingBook();
//        this.activateSmokingFiltering = packet.isActivateSmokingFiltering();
//    }

//    @Override
//    public String getSendingMessage()  {
//        return String.format("Sending %d recipes", this.recipeRegistry.size());
//    }

}
