package com.zenith.feature.inventory.menu;

import com.zenith.cache.data.inventory.Container;
import com.zenith.mc.item.ContainerTypeInfoRegistry;
import com.zenith.mc.item.ItemRegistry;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public abstract class QuickMoveMenu {
    protected final ItemStack[] slots;
    private final ItemStack[] original;

    protected QuickMoveMenu(final int size) {
        this.slots = new ItemStack[size];
        this.original = new ItemStack[size];
    }

    public static @Nullable QuickMoveMenu forContainer(final Container container) {
        final QuickMoveMenu menu = createMenu(container);
        if (menu == null || container.getSize() < menu.size()) {
            return null;
        }
        menu.load(container);
        return menu;
    }

    private static @Nullable QuickMoveMenu createMenu(final Container container) {
        if (container.getContainerId() == 0) {
            return new InventoryQuickMoveMenu();
        }
        var typeInfo = ContainerTypeInfoRegistry.REGISTRY.get(container.getType());
        if (typeInfo == null) {
            return null;
        }
        return switch (container.getType()) {
            case GENERIC_9X1, GENERIC_9X2, GENERIC_9X3, GENERIC_9X4, GENERIC_9X5, GENERIC_9X6 -> new ChestQuickMoveMenu(typeInfo.topSlots());
            case SHULKER_BOX -> new ShulkerBoxQuickMoveMenu(typeInfo.topSlots());
            default -> null;
        };
    }

    private void load(final Container container) {
        for (int i = 0; i < size(); i++) {
            original[i] = container.getItemStack(i);
            slots[i] = original[i] == Container.EMPTY_STACK ? Container.EMPTY_STACK : original[i].clone();
        }
    }

    public int size() {
        return slots.length;
    }

    public boolean canSimulate(final int slot) {
        return slot >= 0 && slot < size();
    }

    public boolean mayPickup(final int slot) {
        return true;
    }

    public Int2ObjectMap<ItemStack> quickMove(final int slot) {
        ItemStack moved = quickMoveStack(slot);
        while (moved != Container.EMPTY_STACK
            && slots[slot] != Container.EMPTY_STACK
            && slots[slot].getId() == moved.getId()) {
            moved = quickMoveStack(slot);
        }

        final Int2ObjectMap<ItemStack> changedSlots = new Int2ObjectArrayMap<>();
        for (int i = 0; i < size(); i++) {
            if (!Objects.equals(original[i], slots[i])) {
                changedSlots.put(i, slots[i]);
            }
        }
        return changedSlots;
    }

    protected abstract ItemStack quickMoveStack(int index);

    protected ItemStack finishQuickMove(final int index, final ItemStack stack, final int originalAmount) {
        if (stack.getAmount() <= 0) {
            slots[index] = Container.EMPTY_STACK;
        }
        return new ItemStack(stack.getId(), originalAmount, stack.getDataComponents());
    }

    protected int slotMaxStackSize(final int slot, final ItemStack stack) {
        return maxStackSize(stack);
    }

    protected boolean slotMayPlace(final int slot, final ItemStack stack) {
        return true;
    }

    protected boolean moveItemStackTo(
        final ItemStack stack,
        final int startIndex,
        final int endIndex,
        final boolean reverseDirection
    ) {
        boolean moved = false;
        int i = reverseDirection ? endIndex - 1 : startIndex;

        if (isStackable(stack)) {
            while (stack.getAmount() > 0 && (reverseDirection ? i >= startIndex : i < endIndex)) {
                final ItemStack target = slots[i];
                if (target != Container.EMPTY_STACK && isSameItemSameComponents(stack, target)) {
                    int combined = target.getAmount() + stack.getAmount();
                    int max = slotMaxStackSize(i, target);
                    if (combined <= max) {
                        stack.setAmount(0);
                        target.setAmount(combined);
                        moved = true;
                    } else if (target.getAmount() < max) {
                        stack.setAmount(stack.getAmount() - (max - target.getAmount()));
                        target.setAmount(max);
                        moved = true;
                    }
                }
                i += reverseDirection ? -1 : 1;
            }
        }

        if (stack.getAmount() > 0) {
            i = reverseDirection ? endIndex - 1 : startIndex;
            while (reverseDirection ? i >= startIndex : i < endIndex) {
                if (slots[i] == Container.EMPTY_STACK && slotMayPlace(i, stack)) {
                    int amount = Math.min(stack.getAmount(), slotMaxStackSize(i, stack));
                    slots[i] = new ItemStack(stack.getId(), amount, stack.getDataComponents());
                    stack.setAmount(stack.getAmount() - amount);
                    moved = true;
                    break;
                }
                i += reverseDirection ? -1 : 1;
            }
        }

        return moved;
    }

    protected static DataComponents effectiveComponents(final ItemStack stack) {
        return stack.withAddedComponents(ItemRegistry.REGISTRY.get(stack.getId()).components()).getDataComponentsOrEmpty();
    }

    protected static int maxStackSize(final ItemStack stack) {
        return effectiveComponents(stack).getOrDefault(DataComponentTypes.MAX_STACK_SIZE, 1);
    }

    protected static boolean isStackable(final ItemStack stack) {
        var components = effectiveComponents(stack);
        boolean isDamageable = components.get(DataComponentTypes.MAX_DAMAGE) != null
            && components.get(DataComponentTypes.UNBREAKABLE) == null
            && components.get(DataComponentTypes.DAMAGE) != null;
        boolean isDamaged = isDamageable && components.getOrDefault(DataComponentTypes.DAMAGE, 0) > 0;
        return components.getOrDefault(DataComponentTypes.MAX_STACK_SIZE, 1) > 1 && !isDamaged;
    }

    protected static boolean isSameItemSameComponents(final ItemStack a, final ItemStack b) {
        return a.getId() == b.getId() && a.getDataComponentsOrEmpty().equals(b.getDataComponentsOrEmpty());
    }
}
