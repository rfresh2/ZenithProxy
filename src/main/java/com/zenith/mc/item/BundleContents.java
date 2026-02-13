package com.zenith.mc.item;

import com.zenith.cache.data.inventory.Container;
import com.zenith.util.struct.Fraction;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.List;

@Data
@AllArgsConstructor
public class BundleContents {
    private static final Fraction BUNDLE_IN_BUNDLE_WEIGHT = Fraction.getFraction(1, 16);

    private final List<ItemStack> items;
    private Fraction weight;
    private int selectedItem;

    public BundleContents(List<ItemStack> content) {
        this(content, computeContentWeight(content), -1);
    }

    private int findStackIndex(ItemStack stack) {
        if (stack == Container.EMPTY_STACK) {
            return -1;
        }

        var itemData = ItemRegistry.REGISTRY.get(stack.getId());
        var components = stack.withAddedComponents(itemData.components()).getDataComponentsOrEmpty();
        var isDamageableItem = components.contains(DataComponentTypes.MAX_DAMAGE)
            && !components.contains(DataComponentTypes.UNBREAKABLE)
            && components.contains(DataComponentTypes.DAMAGE);
        var isDamagedItem = components.getOrDefault(DataComponentTypes.DAMAGE, 0) > 0;
        boolean isStackable = itemData.stackSize() > 1 && (!isDamageableItem || isDamagedItem);

        if (!isStackable) {
            return -1;
        } else {
            for (int i = 0; i < this.items.size(); i++) {
                if (isSameItemSameComponents(this.items.get(i), stack)) {
                    return i;
                }
            }
            return -1;
        }
    }

    private boolean isSameItemSameComponents(final ItemStack stack1, final ItemStack stack2) {
        if (stack1 == Container.EMPTY_STACK || stack2 == Container.EMPTY_STACK) {
            return stack1 == stack2;
        }
        if (stack1.getId() != stack2.getId()) {
            return false;
        } else {
            var stack1Components = stack1.getDataComponentsOrEmpty();
            var stack2Components = stack2.getDataComponents();
            return stack1Components.equals(stack2Components);
        }
    }

    private int getMaxAmountToAdd(ItemStack stack) {
        Fraction fraction = Fraction.ONE.subtract(this.weight);
        return Math.max(fraction.divideBy(BundleContents.getWeight(stack)).intValue(), 0);
    }

    public int tryInsert(ItemStack stack) {
        if (!BundleContents.canItemBeInBundle(stack)) {
            return 0;
        } else {
            int i = Math.min(stack.getAmount(), this.getMaxAmountToAdd(stack));
            if (i == 0) {
                return 0;
            } else {
                this.weight = this.weight.add(BundleContents.getWeight(stack).multiplyBy(Fraction.getFraction(i, 1)));
                int j = this.findStackIndex(stack);
                if (j != -1) {
                    ItemStack itemStack = this.items.remove(j);
                    ItemStack itemStack2 = new ItemStack(itemStack.getId(), itemStack.getAmount() + i, itemStack.getDataComponents());
                    // todo: should we actually be self-mutating the stack here?
                    stack.setAmount(stack.getAmount() - i);
                    this.items.addFirst(itemStack2);
                } else {
                    int i2 = Math.min(i, stack.getAmount());
                    ItemStack itemStack = new ItemStack(stack.getId(), i2, stack.getDataComponents());
                    stack.setAmount(stack.getAmount() - i2);
                    this.items.addFirst(itemStack);
                }

                return i;
            }
        }
    }

    public void toggleSelectedItem(int selectedItem) {
        this.selectedItem = this.selectedItem != selectedItem && selectedItem < this.items.size() ? selectedItem : -1;
    }

    public ItemStack removeOne() {
        if (this.items.isEmpty()) {
            return null;
        } else {
            int i = this.selectedItem != -1 && this.selectedItem < this.items.size() ? this.selectedItem : 0;
            ItemStack itemStack = this.items.remove(i);
            itemStack = new ItemStack(itemStack.getId(), itemStack.getAmount(), itemStack.getDataComponents());
            this.weight = this.weight.subtract(BundleContents.getWeight(itemStack).multiplyBy(Fraction.getFraction(itemStack.getAmount(), 1)));
            this.toggleSelectedItem(-1);
            return itemStack;
        }
    }

    public static boolean canItemBeInBundle(ItemStack stack) {
        return stack != Container.EMPTY_STACK && !ItemRegistry.REGISTRY.get(stack.getId()).itemTags().contains(ItemTags.SHULKER_BOXES);
    }

    public static Fraction computeContentWeight(List<ItemStack> content) {
        Fraction fraction = Fraction.ZERO;

        for (ItemStack itemStack : content) {
            fraction = fraction.add(getWeight(itemStack).multiplyBy(Fraction.getFraction(itemStack.getAmount(), 1)));
        }

        return fraction;
    }

    public static Fraction getWeight(@NonNull ItemStack stack) {
        var stackComponents = stack.getDataComponents();
        if (stackComponents != null) {
            var bundleComponent = stackComponents.get(DataComponentTypes.BUNDLE_CONTENTS);
            if (bundleComponent != null) {
                var bundleContents = new BundleContents(bundleComponent);
                return BUNDLE_IN_BUNDLE_WEIGHT.add(bundleContents.getWeight());
            }
            var beesComponent = stack.getDataComponents().getOrDefault(DataComponentTypes.BEES, Collections.emptyList());
            return !beesComponent.isEmpty()
                ? Fraction.ONE
                : Fraction.getFraction(1, ItemRegistry.REGISTRY.get(stack.getId()).stackSize());
        }
        return Fraction.getFraction(1, ItemRegistry.REGISTRY.get(stack.getId()).stackSize());
    }
}
