package com.zenith.util;

import com.zenith.mc.item.ItemRegistry;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;

public class ItemUtil {
    private ItemUtil() {}

    public static int getDamageValue(ItemStack itemStack) {
        int defaultDamage = 0;
        if (itemStack == null) return defaultDamage;
        var dataComponents = itemStack.getDataComponentsOrEmpty();
        var damageComponent = dataComponents.get(DataComponentTypes.DAMAGE);
        if (damageComponent == null) return defaultDamage;
        return damageComponent;
    }

    public static int getMaxDamage(ItemStack itemStack) {
        int defaultMaxDamage = 0;
        if (itemStack == null) return defaultMaxDamage;
        var itemData = ItemRegistry.REGISTRY.get(itemStack.getId());
        if (itemData == null) return defaultMaxDamage;
        var defaultMaxDamageComponent = itemData.components().get(DataComponentTypes.MAX_DAMAGE);
        if (defaultMaxDamageComponent == null) return defaultMaxDamage;
        return defaultMaxDamageComponent;
    }

    public static int getDamageUntilBreak(ItemStack itemStack) {
        return getMaxDamage(itemStack) - getDamageValue(itemStack);
    }
}
