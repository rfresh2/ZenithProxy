package com.zenith.module.impl;

import com.zenith.cache.data.inventory.Container;
import com.zenith.event.module.ClientBotTick;
import com.zenith.feature.items.ItemsData;
import com.zenith.module.Module;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;
import static java.util.Arrays.asList;

public class AutoArmor extends Module {
    private int delay = 0;
    public static final int MOVEMENT_PRIORITY = 1000;
    private static List<EquipmentSlot> ARMOR_SLOTS = asList(EquipmentSlot.HELMET, EquipmentSlot.CHESTPLATE, EquipmentSlot.LEGGINGS, EquipmentSlot.BOOTS);

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(
            this,
            of(ClientBotTick.class, this::handleClientBotTick),
            of(ClientBotTick.Starting.class, this::handleClientBotTickStarting)
        );
    }

    @Override
    public boolean shouldBeEnabled() {
        return CONFIG.client.extra.autoArmor.enabled;
    }

    private void handleClientBotTickStarting(ClientBotTick.Starting starting) {
        delay = 0;
    }

    private void handleClientBotTick(ClientBotTick clientBotTick) {
        if (MODULE.get(AutoEat.class).isEating()) return;
        if (delay > 0) {
            delay--;
            return;
        }
        for (EquipmentSlot equipmentSlot : ARMOR_SLOTS) {
            // identify if we have the best possible armor equipped for the slot
            final ItemStack currentItemStack = CACHE.getPlayerCache().getEquipment(equipmentSlot);
            final int invSlotId = ARMOR_SLOTS.indexOf(equipmentSlot) + 5;
            final BestArmorData bestArmorInInventory = getBestArmorInInventory(equipmentSlot);
            if (bestArmorInInventory == null) continue;
            if (currentItemStack == Container.EMPTY_STACK) {
                INVENTORY.invActionReq(this, INVENTORY.swapSlots(bestArmorInInventory.index(), invSlotId), MOVEMENT_PRIORITY);
                delay = 5;
                return;
            }
            final ArmorMaterial currentArmorMaterial = getArmorMaterial(ITEMS.getItemData(currentItemStack.getId()));
            if (currentArmorMaterial == null || bestArmorInInventory.material().compareTo(currentArmorMaterial) > 0) {
                INVENTORY.invActionReq(this, INVENTORY.swapSlots(bestArmorInInventory.index(), invSlotId), MOVEMENT_PRIORITY);
                delay = 5;
                return;
            }
        }
        delay = 50; // delay processing for a bit as its unlikely our inventory changed
    }

    private record BestArmorData(ItemStack itemStack, int index, ItemsData itemsData, ArmorMaterial material) {}

    private BestArmorData getBestArmorInInventory(final EquipmentSlot equipmentSlot) {
        int bestArmorIndex = -1;
        ItemsData bestArmorItemData = null;
        ArmorMaterial bestArmorMaterial = null;
        String equipmentTypeItemNameSuffix = ("_" + equipmentSlot.name()).toLowerCase();
        final List<ItemStack> inv = CACHE.getPlayerCache().getPlayerInventory();
        for (int i = 9; i <= 44; i++) {
            if (inv.size() <= i) break;
            final ItemStack stack = inv.get(i);
            if (stack == Container.EMPTY_STACK) continue;
            final ItemsData itemData = ITEMS.getItemData(stack.getId());
            if (itemData == null) continue;
            if (!itemData.getName().toLowerCase().endsWith(equipmentTypeItemNameSuffix)) continue;
            final ArmorMaterial armorMaterial = getArmorMaterial(itemData);
            if (armorMaterial == null) continue;
            if (bestArmorMaterial == null || armorMaterial.compareTo(bestArmorMaterial) > 0) {
                bestArmorMaterial = armorMaterial;
                bestArmorItemData = itemData;
                bestArmorIndex = i;
            }
        }
        if (bestArmorIndex != -1) {
            return new BestArmorData(inv.get(bestArmorIndex), bestArmorIndex, bestArmorItemData, bestArmorMaterial);
        }
        return null;
    }

    private @Nullable ArmorMaterial getArmorMaterial(final ItemsData itemData) {
        final int underscoreIndex = itemData.getName().indexOf("_");
        if (underscoreIndex == -1) return null;
        String materialName = itemData.getName().substring(0, underscoreIndex).toUpperCase(Locale.ROOT);
        return ArmorMaterial.valueOf(materialName);
    }

    private static class ArmorMaterial implements Comparable<ArmorMaterial> {
        static ArmorMaterial LEATHER = new ArmorMaterial(0);
        static ArmorMaterial GOLD = new ArmorMaterial(1);
        static ArmorMaterial CHAIN = new ArmorMaterial(2);
        static ArmorMaterial IRON = new ArmorMaterial(3);
        static ArmorMaterial DIAMOND = new ArmorMaterial(4);
        static ArmorMaterial NETHERITE = new ArmorMaterial(5);
        private final int priority;

        ArmorMaterial(int priority) {
            this.priority = priority;
        }

        public static @Nullable ArmorMaterial valueOf(final String materialName) {
            switch (materialName) {
                case "LEATHER":
                    return LEATHER;
                case "GOLD":
                    return GOLD;
                case "CHAIN":
                    return CHAIN;
                case "IRON":
                    return IRON;
                case "DIAMOND":
                    return DIAMOND;
                case "NETHERITE":
                    return NETHERITE;
            }
            return null;
        }

        public int compareTo(ArmorMaterial o) {
            if (o == null) return 1;
            return Integer.compare(this.priority, o.priority);
        }
    }
}
