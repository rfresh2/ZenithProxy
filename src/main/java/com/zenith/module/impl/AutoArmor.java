package com.zenith.module.impl;

import com.github.rfresh2.EventConsumer;
import com.zenith.cache.data.inventory.Container;
import com.zenith.event.client.ClientBotTick;
import com.zenith.feature.inventory.InventoryActionRequest;
import com.zenith.feature.inventory.util.InventoryActionMacros;
import com.zenith.mc.item.ItemData;
import com.zenith.mc.item.ItemRegistry;
import com.zenith.module.api.Module;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;
import static java.util.Arrays.asList;

public class AutoArmor extends Module {
    private int delay = 0;
    private static final List<EquipmentSlot> ARMOR_SLOTS = asList(EquipmentSlot.HELMET, EquipmentSlot.CHESTPLATE, EquipmentSlot.LEGGINGS, EquipmentSlot.BOOTS);

    @Override
    public List<EventConsumer<?>> registerEvents() {
        return List.of(
            of(ClientBotTick.class, this::handleClientBotTick),
            of(ClientBotTick.Starting.class, this::handleClientBotTickStarting)
        );
    }

    @Override
    public boolean enabledSetting() {
        return CONFIG.client.extra.autoArmor.enabled;
    }

    public int getPriority() {
        return Objects.requireNonNullElse(CONFIG.client.extra.autoArmor.priority, 12000);
    }

    private void handleClientBotTickStarting(ClientBotTick.Starting starting) {
        delay = 0;
    }

    private void handleClientBotTick(ClientBotTick clientBotTick) {
        if (delay > 0) {
            delay--;
            return;
        }
        for (int i = 0; i < ARMOR_SLOTS.size(); i++) {
            final EquipmentSlot equipmentSlot = ARMOR_SLOTS.get(i);
            // identify if we have the best possible armor equipped for the slot
            final ItemStack currentItemStack = CACHE.getPlayerCache().getEquipment(equipmentSlot);
            final int invSlotId = ARMOR_SLOTS.indexOf(equipmentSlot) + 5;
            final BestArmorData bestArmorInInventory = getBestArmorInInventory(equipmentSlot);
            if (bestArmorInInventory == null) continue;
            if (currentItemStack == Container.EMPTY_STACK) {
                INVENTORY.submit(InventoryActionRequest.builder()
                    .owner(this)
                    .actions(InventoryActionMacros.swapSlots(bestArmorInInventory.index(), invSlotId))
                    .priority(getPriority())
                    .build());
                delay = 5;
                return;
            }
            final ArmorMaterial currentArmorMaterial = getArmorMaterial(ItemRegistry.REGISTRY.get(currentItemStack.getId()));
            if (currentArmorMaterial == null || bestArmorInInventory.material().compareTo(currentArmorMaterial) > 0) {
                INVENTORY.submit(InventoryActionRequest.builder()
                    .owner(this)
                    .actions(InventoryActionMacros.swapSlots(bestArmorInInventory.index(), invSlotId))
                    .priority(getPriority())
                    .build());
                delay = 5;
                return;
            }
        }
        delay = 50; // delay processing for a bit as its unlikely our inventory changed
    }

    private record BestArmorData(ItemStack itemStack, int index, ItemData itemsData, ArmorMaterial material) {}

    private BestArmorData getBestArmorInInventory(final EquipmentSlot equipmentSlot) {
        int bestArmorIndex = -1;
        ItemData bestArmorItemData = null;
        ArmorMaterial bestArmorMaterial = null;
        String equipmentTypeItemNameSuffix = ("_" + equipmentSlot.name()).toLowerCase();
        final List<ItemStack> inv = CACHE.getPlayerCache().getPlayerInventory();
        for (int i = 9; i <= 44; i++) {
            if (inv.size() <= i) break;
            final ItemStack stack = inv.get(i);
            if (stack == Container.EMPTY_STACK) continue;
            final ItemData itemData = ItemRegistry.REGISTRY.get(stack.getId());
            if (itemData == null) continue;
            if (!itemData.name().toLowerCase().endsWith(equipmentTypeItemNameSuffix)) continue;
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

    private @Nullable ArmorMaterial getArmorMaterial(final ItemData itemData) {
        final int underscoreIndex = itemData.name().indexOf("_");
        if (underscoreIndex == -1) return null;
        String materialName = itemData.name().substring(0, underscoreIndex).toUpperCase(Locale.ROOT);
        return ArmorMaterial.valueOf(materialName);
    }

    private record ArmorMaterial(int priority) implements Comparable<ArmorMaterial> {
            static final ArmorMaterial LEATHER = new ArmorMaterial(0);
            static final ArmorMaterial GOLD = new ArmorMaterial(1);
            static final ArmorMaterial CHAIN = new ArmorMaterial(2);
            static final ArmorMaterial IRON = new ArmorMaterial(3);
            static final ArmorMaterial DIAMOND = new ArmorMaterial(4);
            static final ArmorMaterial NETHERITE = new ArmorMaterial(5);

        public static @Nullable ArmorMaterial valueOf(final String materialName) {
            return switch (materialName) {
                case "LEATHER" -> LEATHER;
                case "GOLD" -> GOLD;
                case "CHAIN" -> CHAIN;
                case "IRON" -> IRON;
                case "DIAMOND" -> DIAMOND;
                case "NETHERITE" -> NETHERITE;
                default -> null;
            };
        }

            public int compareTo(ArmorMaterial o) {
                if (o == null) return 1;
                return Integer.compare(this.priority, o.priority);
            }
        }
}
