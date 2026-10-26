package com.zenith.feature.inventory.menu;

import com.zenith.mc.item.ItemRegistry;
import com.zenith.mc.item.ItemTags;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;

public class ShulkerBoxQuickMoveMenu extends ChestQuickMoveMenu {
    public ShulkerBoxQuickMoveMenu(final int topSlots) {
        super(topSlots);
    }

    @Override
    protected boolean slotMayPlace(final int slot, final ItemStack stack) {
        if (slot < topSlots) {
            return !ItemRegistry.REGISTRY.get(stack.getId()).itemTags().contains(ItemTags.SHULKER_BOXES);
        }
        return true;
    }
}
