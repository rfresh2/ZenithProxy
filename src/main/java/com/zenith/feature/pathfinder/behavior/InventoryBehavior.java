package com.zenith.feature.pathfinder.behavior;

import com.zenith.cache.data.inventory.Container;
import com.zenith.feature.inventory.InventoryActionRequest;
import com.zenith.feature.inventory.actions.MoveToHotbarSlot;
import com.zenith.feature.inventory.actions.SetHeldItem;
import com.zenith.feature.inventory.util.InventoryUtil;
import com.zenith.feature.pathfinder.Baritone;
import com.zenith.mc.block.BlockRegistry;
import com.zenith.mc.item.ItemData;
import com.zenith.mc.item.ItemRegistry;
import com.zenith.mc.item.ToolType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.MoveToHotbarAction;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;

import java.util.List;
import java.util.function.Predicate;

import static com.zenith.Globals.*;

public class InventoryBehavior extends Behavior {

    int ticksSinceLastInventoryMove;
    int[] lastTickRequestedMove; // not everything asks every tick, so remember the request while coming to a halt

    public InventoryBehavior(Baritone baritone) {
        super(baritone);
    }

    public void onTick() {
        if (!CONFIG.client.extra.pathfinder.allowInventory) {
            return;
        }
        if (CACHE.getPlayerCache().getInventoryCache().getOpenContainerId() != 0) {
            // we have a crafting table or a chest or something open
            return;
        }
        ticksSinceLastInventoryMove++;
        int throwawayIndex = firstValidThrowaway();
        if (throwawayIndex > -1 && throwawayIndex < 36) { // aka there are none on the hotbar, but there are some in main inventory
            requestSwapWithHotBar(throwawayIndex, 8);
        }
        int pickIndex = InventoryUtil.bestToolAgainst(BlockRegistry.STONE);
        if (pickIndex > -1 && pickIndex < 36) {
            requestSwapWithHotBar(pickIndex, 6);
        }
        if (lastTickRequestedMove != null) {
            PATH_LOG.debug("Remembering to move {} {} from a previous tick", lastTickRequestedMove[0], lastTickRequestedMove[1]);
            requestSwapWithHotBar(lastTickRequestedMove[0], lastTickRequestedMove[1]);
        }
    }

//    public boolean attemptToPutOnHotbar(int inMainInvy, Predicate<Integer> disallowedHotbar) {
//        OptionalInt destination = getTempHotbarSlot(disallowedHotbar);
//        if (destination.isPresent()) {
//            if (!requestSwapWithHotBar(inMainInvy, destination.getAsInt())) {
//                return false;
//            }
//        }
//        return true;
//    }
//
//    public OptionalInt getTempHotbarSlot(Predicate<Integer> disallowedHotbar) {
//        // we're using 0 and 8 for pickaxe and throwaway
//        ArrayList<Integer> candidates = new ArrayList<>();
//        for (int i = 1; i < 8; i++) {
//            if (ctx.player().getInventory().items.get(i).isEmpty() && !disallowedHotbar.test(i)) {
//                candidates.add(i);
//            }
//        }
//        if (candidates.isEmpty()) {
//            for (int i = 1; i < 8; i++) {
//                if (!disallowedHotbar.test(i)) {
//                    candidates.add(i);
//                }
//            }
//        }
//        if (candidates.isEmpty()) {
//            return OptionalInt.empty();
//        }
//        return OptionalInt.of(candidates.get(new Random().nextInt(candidates.size())));
//    }

    private boolean requestSwapWithHotBar(int inInventory, int inHotbar) {
        lastTickRequestedMove = new int[]{inInventory, inHotbar};
        if (ticksSinceLastInventoryMove < 1) {// Baritone.settings().ticksBetweenInventoryMoves.value) {
            PATH_LOG.debug("Inventory move requested but delaying " + ticksSinceLastInventoryMove + " " + "1"); // Baritone.settings().ticksBetweenInventoryMoves.value);
            return false;
        }
//        if (Baritone.settings().inventoryMoveOnlyIfStationary.value && !baritone.getInventoryPauserProcess().stationaryForInventoryMove()) {
//            PATH_LOG.info("Inventory move requested but delaying until stationary");
//            return false;
//        }
        INVENTORY.submit(InventoryActionRequest.builder()
             .owner(this)
             .actions(new MoveToHotbarSlot(inInventory, MoveToHotbarAction.from(inHotbar)))
             .priority(Baritone.getPriority())
             .build());
        ticksSinceLastInventoryMove = 0;
        lastTickRequestedMove = null;
        return true;
    }

    private int firstValidThrowaway() { // TODO offhand idk
        List<ItemStack> playerInventory = CACHE.getPlayerCache().getPlayerInventory();
        for (int i = 44; i >= 9; i--) {
            ItemStack itemStack = playerInventory.get(i);
            if (itemStack == Container.EMPTY_STACK) continue;
            var itemData = ItemRegistry.REGISTRY.get(itemStack.getId());
            if (itemData == null) continue;
            if (CONFIG.client.extra.pathfinder.acceptableThrowawayItems.contains(itemData.name())) {
                return i;
            }
        }
        return -1;
    }

    public boolean hasGenericThrowaway() {
        int i = firstValidThrowaway();
        if (i == -1) {
            return false;
        }
        return true;
    }

    public boolean selectThrowawayForLocation(boolean select, int x, int y, int z) {
//        BlockState maybe = baritone.getBuilderProcess().placeAt(x, y, z, baritone.bsi.get0(x, y, z));
//        if (maybe != null && throwaway(select, stack -> stack.getItem() instanceof BlockItem && maybe.equals(((BlockItem) stack.getItem()).getBlock().getStateForPlacement(new BlockPlaceContext(new UseOnContext(ctx.world(), ctx.player(), InteractionHand.MAIN_HAND, stack, new BlockHitResult(new Vec3(ctx.player().position().x, ctx.player().position().y, ctx.player().position().z), Direction.UP, ctx.playerFeet(), false)) {}))))) {
//            return true; // gotem
//        }
//        if (maybe != null && throwaway(select, stack -> stack.getItem() instanceof BlockItem && ((BlockItem) stack.getItem()).getBlock().equals(maybe.getBlock()))) {
//            return true;
//        }
        for (var itemName : CONFIG.client.extra.pathfinder.acceptableThrowawayItems) {
            if (throwaway(select, stack -> {
                if (stack == Container.EMPTY_STACK) return false;
                var itemData = ItemRegistry.REGISTRY.get(stack.getId());
                if (itemData == null) return false;
                return itemName.equals(itemData.name());
            })) {
                return true;
            }
        }
        return false;
    }

    public boolean throwaway(boolean select, Predicate<? super ItemStack> desired) {
        return throwaway(select, desired, CONFIG.client.extra.pathfinder.allowInventory);
    }

    public boolean throwaway(boolean select, Predicate<? super ItemStack> desired, boolean allowInventory) {
        List<ItemStack> inv = CACHE.getPlayerCache().getPlayerInventory();
        for (int i = 44; i >= 36; i--) {
            ItemStack item = inv.get(i);
            if (item == Container.EMPTY_STACK) continue;
            // this usage of settings() is okay because it's only called once during pathing
            // (while creating the CalculationContext at the very beginning)
            // and then it's called during execution
            // since this function is never called during cost calculation, we don't need to migrate
            // acceptableThrowawayItems to the CalculationContext
            if (desired.test(item)) {
                if (select) {
                    int hotbarIndex = i - 36;
                    if (CACHE.getPlayerCache().getHeldItemSlot() != hotbarIndex) {
                        INVENTORY.submit(InventoryActionRequest.builder()
                            .owner(this)
                            .actions(new SetHeldItem(hotbarIndex))
                            .priority(Baritone.getPriority())
                            .build());
                    }
                }
                return true;
            }
        }
        ItemStack offhand = CACHE.getPlayerCache().getEquipment(EquipmentSlot.OFF_HAND);
        if (offhand != Container.EMPTY_STACK && desired.test(offhand)) {
            // main hand takes precedence over off hand
            // that means that if we have block A selected in main hand and block B in off hand, right clicking places block B
            // we've already checked above ^ and the main hand can't possible have an acceptablethrowawayitem
            // so we need to select in the main hand something that doesn't right click
            // so not a shovel, not a hoe, not a block, etc
            for (int i = 44; i >= 36; i--) {
                ItemStack item = inv.get(i);
                if (item == Container.EMPTY_STACK) {
                    int hotbarIndex = i - 36;
                    if (CACHE.getPlayerCache().getHeldItemSlot() != hotbarIndex) {
                        INVENTORY.submit(InventoryActionRequest.builder()
                            .owner(this)
                            .actions(new SetHeldItem(hotbarIndex))
                            .priority(Baritone.getPriority())
                            .build());
                    }
                } else {
                    ItemData itemData = ItemRegistry.REGISTRY.get(item.getId());
                    if (itemData.toolTag() != null && itemData.toolTag().type() == ToolType.PICKAXE) {
                        int hotbarIndex = i - 36;
                        if (CACHE.getPlayerCache().getHeldItemSlot() != hotbarIndex) {
                            INVENTORY.submit(InventoryActionRequest.builder()
                                .owner(this)
                                .actions(new SetHeldItem(hotbarIndex))
                                .priority(Baritone.getPriority())
                                .build());
                        }
                        return true;
                    }
                }
            }
        }

        if (allowInventory) {
            for (int i = 9; i < 36; i++) {
                ItemStack itemStack = inv.get(i);
                if (itemStack == Container.EMPTY_STACK) continue;
                if (desired.test(itemStack)) {
                    if (select) {
                        requestSwapWithHotBar(i, 7);
                        if (CACHE.getPlayerCache().getHeldItemSlot() != 7) {
                            INVENTORY.submit(InventoryActionRequest.builder()
                                .owner(this)
                                .actions(new SetHeldItem(7))
                                .priority(Baritone.getPriority())
                                .build());
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }
}
