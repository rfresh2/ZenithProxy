package com.zenith.module.impl;

import com.zenith.cache.data.inventory.Container;
import com.zenith.feature.inventory.InventoryActionRequest;
import com.zenith.feature.inventory.actions.*;
import com.zenith.module.api.Module;
import lombok.Getter;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ClickItemAction;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.MoveToHotbarAction;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.INVENTORY;
import static java.util.Objects.nonNull;

/**
 * Modules that follow a general pattern of equipping an item to a hotbar or offhand slot and using it
 */
public abstract class AbstractInventoryModule extends Module {
    private final HandRestriction handRestriction;
    private final int targetMainHandHotbarSlot;
    @Getter
    private @Nullable Hand hand = null;

    public AbstractInventoryModule(HandRestriction handRestriction, int targetMainHandHotbarSlot) {
        this.handRestriction = handRestriction;
        this.targetMainHandHotbarSlot = targetMainHandHotbarSlot;
    }

    public abstract boolean itemPredicate(ItemStack itemStack);

    public abstract int getPriority();

    public enum HandRestriction {
        MAIN_HAND,
        OFF_HAND,
        EITHER
    }

    // returns delay (if any) before next action
    public int doInventoryActions() {
        if (isItemEquipped()) return 0;
        if (switchToItem()) return 5;
        return 0;
    }

    public boolean isItemEquipped() {
        if (handRestriction == HandRestriction.EITHER || handRestriction == HandRestriction.OFF_HAND) {
            final ItemStack offhandStack = CACHE.getPlayerCache().getEquipment(EquipmentSlot.OFF_HAND);
            var offHandEquipped = nonNull(offhandStack) && itemPredicate(offhandStack);
            if (offHandEquipped) {
                hand = Hand.OFF_HAND;
                return true;
            }
        }
        if (handRestriction == HandRestriction.EITHER || handRestriction == HandRestriction.MAIN_HAND) {
            final ItemStack mainHandStack = CACHE.getPlayerCache().getEquipment(EquipmentSlot.MAIN_HAND);
            var mainHandEquipped = nonNull(mainHandStack) && itemPredicate(mainHandStack);
            if (mainHandEquipped) {
                hand = Hand.MAIN_HAND;
                return true;
            }
        }
        hand = null;
        return false;
    }

    private MoveToHotbarAction getActionSlot() {
        if (handRestriction == HandRestriction.OFF_HAND) return MoveToHotbarAction.OFF_HAND;
        return MoveToHotbarAction.from(targetMainHandHotbarSlot);
    }

    // assumes we've already tested that the item is not equipped
    // returns true if we performed an item swap
    public boolean switchToItem() {
        // find next and switch it to our hotbar slot
        final List<ItemStack> inventory = CACHE.getPlayerCache().getPlayerInventory();
        for (int i = 44; i >= 9; i--) {
            ItemStack itemStack = inventory.get(i);
            if (nonNull(itemStack) && itemPredicate(itemStack)) {
                List<InventoryAction> actions = new ArrayList<>();
                var openContainer = CACHE.getPlayerCache().getInventoryCache().getOpenContainerId();
                if (openContainer != 0) {
                    actions.add(new CloseContainer(openContainer));
                }
                if (CACHE.getPlayerCache().getInventoryCache().getMouseStack() != Container.EMPTY_STACK) {
                    actions.add(new DropMouseStack(ClickItemAction.LEFT_CLICK));
                }
                var actionSlot = getActionSlot();
                actions.add(new MoveToHotbarSlot(i, actionSlot));
                if (actionSlot != MoveToHotbarAction.OFF_HAND) {
                    actions.add(new SetHeldItem(targetMainHandHotbarSlot));
                }
                INVENTORY.submit(InventoryActionRequest.builder()
                    .owner(this)
                    .actions(actions)
                    .priority(getPriority())
                    .build());
                return true;
            }
        }
        return false;
    }
}
