package com.zenith.feature.inventory.menu;

import com.zenith.cache.data.inventory.Container;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;

public class ChestQuickMoveMenu extends QuickMoveMenu {
    protected final int topSlots;

    public ChestQuickMoveMenu(final int topSlots) {
        super(topSlots + 36);
        this.topSlots = topSlots;
    }

    @Override
    protected ItemStack quickMoveStack(final int index) {
        final ItemStack stack = slots[index];
        if (stack == Container.EMPTY_STACK) {
            return Container.EMPTY_STACK;
        }
        final int originalAmount = stack.getAmount();
        final boolean moved = index < topSlots
            ? moveItemStackTo(stack, topSlots, size(), true)
            : moveItemStackTo(stack, 0, topSlots, false);
        if (!moved) {
            return Container.EMPTY_STACK;
        }
        return finishQuickMove(index, stack, originalAmount);
    }
}
