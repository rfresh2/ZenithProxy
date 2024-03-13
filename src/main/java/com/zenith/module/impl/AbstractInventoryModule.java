package com.zenith.module.impl;

import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.inventory.ContainerActionType;
import com.github.steveice10.mc.protocol.data.game.inventory.MoveToHotbarAction;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundSetCarriedItemPacket;
import com.zenith.feature.items.ContainerClickAction;
import com.zenith.module.Module;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.zenith.Shared.*;
import static java.util.Objects.nonNull;

/**
 * Modules that follow a general pattern of equipping an item to a hotbar or offhand slot and using it
 */
public abstract class AbstractInventoryModule extends Module {
    private final boolean onlyOffhand;
    private final int targetMainHandHotbarSlot;
    private final int inventoryActionPriority;
    @Getter
    private @Nullable Hand hand = null;

    public AbstractInventoryModule(boolean onlyOffhand, int targetMainHandHotbarSlot, int inventoryActionPriority) {
        this.onlyOffhand = onlyOffhand;
        this.targetMainHandHotbarSlot = targetMainHandHotbarSlot;
        this.inventoryActionPriority = inventoryActionPriority;
    }

    public abstract boolean itemPredicate(ItemStack itemStack);

    // returns delay (if any) before next action
    public int doInventoryActions() {
        if (isItemEquipped()) return 0;
        if (switchToItem()) return 5;
        return 0;
    }

    public boolean isItemEquipped() {
        final ItemStack offhandStack = CACHE.getPlayerCache().getEquipment(EquipmentSlot.OFF_HAND);
        var offHandEquipped = nonNull(offhandStack) && itemPredicate(offhandStack);
        if (offHandEquipped) {
            hand = Hand.OFF_HAND;
            return true;
        }
        if (onlyOffhand) return false;
        final ItemStack mainHandStack = CACHE.getPlayerCache().getEquipment(EquipmentSlot.MAIN_HAND);
        var mainHandEquipped = nonNull(mainHandStack) && itemPredicate(mainHandStack);
        if (mainHandEquipped) {
            hand = Hand.MAIN_HAND;
            return true;
        }
        hand = null;
        return false;
    }

    private MoveToHotbarAction getActionSlot() {
        if (onlyOffhand) return MoveToHotbarAction.OFF_HAND;
        return MoveToHotbarAction.from(targetMainHandHotbarSlot);
    }

    // assumes we've already tested that the item is not equipped
    // returns true if we performed an item swap
    public boolean switchToItem() {
        // find next food and switch it to our hotbar slot
        final List<ItemStack> inventory = CACHE.getPlayerCache().getPlayerInventory();
        for (int i = 44; i >= 9; i--) {
            ItemStack itemStack = inventory.get(i);
            if (nonNull(itemStack) && itemPredicate(itemStack)) {
                var actionSlot = getActionSlot();
                PLAYER_INVENTORY_MANAGER.invActionReq(
                    this,
                    new ContainerClickAction(i, ContainerActionType.MOVE_TO_HOTBAR_SLOT, actionSlot),
                    inventoryActionPriority
                );
                CLIENT_LOG.debug("[{}] Swapping item to slot {}", getClass().getSimpleName(), actionSlot.getId());
                if (actionSlot != MoveToHotbarAction.OFF_HAND) {
                    if (CACHE.getPlayerCache().getHeldItemSlot() != targetMainHandHotbarSlot) {
                        sendClientPacketAsync(new ServerboundSetCarriedItemPacket(targetMainHandHotbarSlot));
                    }
                }
                return true;
            }
        }
        return false;
    }
}
