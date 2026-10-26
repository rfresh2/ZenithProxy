package com.zenith.feature.inventory.menu;

import com.zenith.cache.data.inventory.Container;
import com.zenith.mc.enchantment.EnchantmentRegistry;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;

import static com.zenith.Globals.CACHE;

public class InventoryQuickMoveMenu extends QuickMoveMenu {

    public InventoryQuickMoveMenu() {
        super(46);
    }

    // crafting result requires recipe simulation
    @Override
    public boolean canSimulate(final int slot) {
        return super.canSimulate(slot) && slot != 0;
    }

    @Override
    public boolean mayPickup(final int slot) {
        return !isArmorSlot(slot) || slots[slot] == Container.EMPTY_STACK || !preventsArmorChange(slots[slot]);
    }

    @Override
    protected ItemStack quickMoveStack(final int index) {
        final ItemStack stack = slots[index];
        if (stack == Container.EMPTY_STACK) {
            return Container.EMPTY_STACK;
        }
        final int originalAmount = stack.getAmount();
        final EquipmentSlot equipmentSlot = equipmentSlotForItem(stack);

        final boolean moved;
        if (index >= 1 && index < 9) {
            moved = moveItemStackTo(stack, 9, 45, false);
        } else if (isArmorEquipmentSlot(equipmentSlot) && slots[armorSlotIndex(equipmentSlot)] == Container.EMPTY_STACK) {
            int armorSlot = armorSlotIndex(equipmentSlot);
            moved = moveItemStackTo(stack, armorSlot, armorSlot + 1, false);
        } else if (equipmentSlot == EquipmentSlot.OFF_HAND && slots[45] == Container.EMPTY_STACK) {
            moved = moveItemStackTo(stack, 45, 45 + 1, false);
        } else if (index >= 9 && index < 36) {
            moved = moveItemStackTo(stack, 36, 45, false);
        } else if (index >= 36 && index < 45) {
            moved = moveItemStackTo(stack, 9, 36, false);
        } else {
            moved = moveItemStackTo(stack, 9, 45, false);
        }
        if (!moved || stack.getAmount() == originalAmount) return Container.EMPTY_STACK;
        return finishQuickMove(index, stack, originalAmount);
    }

    @Override
    protected int slotMaxStackSize(final int slot, final ItemStack stack) {
        int itemMax = super.slotMaxStackSize(slot, stack);
        return isArmorSlot(slot) ? Math.min(1, itemMax) : itemMax;
    }

    @Override
    protected boolean slotMayPlace(final int slot, final ItemStack stack) {
        if (isArmorSlot(slot)) {
            return equipmentSlotForItem(stack) == armorEquipmentSlot(slot);
        }
        return true;
    }

    private static boolean isArmorSlot(final int slot) {
        return slot >= 4 && slot <= 8;
    }

    private static boolean isArmorEquipmentSlot(final EquipmentSlot slot) {
        return switch (slot) {
            case HELMET, CHESTPLATE, LEGGINGS, BOOTS -> true;
            default -> false;
        };
    }

    private static int armorSlotIndex(final EquipmentSlot slot) {
        return switch (slot) {
            case HELMET -> 5;
            case CHESTPLATE -> 6;
            case LEGGINGS -> 7;
            case BOOTS -> 8;
            default -> throw new IllegalArgumentException("Not an armor slot: " + slot);
        };
    }

    private static EquipmentSlot armorEquipmentSlot(final int slot) {
        return switch (slot) {
            case 5 -> EquipmentSlot.HELMET;
            case 6 -> EquipmentSlot.CHESTPLATE;
            case 7 -> EquipmentSlot.LEGGINGS;
            case 8 -> EquipmentSlot.BOOTS;
            default -> throw new IllegalArgumentException("Not an armor slot: " + slot);
        };
    }

    private static EquipmentSlot equipmentSlotForItem(final ItemStack stack) {
        var equippable = effectiveComponents(stack).get(DataComponentTypes.EQUIPPABLE);
        if (equippable == null || equippable.slot() == EquipmentSlot.BODY) {
            return EquipmentSlot.MAIN_HAND;
        }
        return equippable.slot();
    }

    private static boolean preventsArmorChange(final ItemStack stack) {
        if (CACHE.getPlayerCache().getGameMode() == GameMode.CREATIVE) {
            return false;
        }
        var enchantments = effectiveComponents(stack).get(DataComponentTypes.ENCHANTMENTS);
        return enchantments != null && enchantments.getEnchantments().containsKey(EnchantmentRegistry.BINDING_CURSE.get().id());
    }
}
