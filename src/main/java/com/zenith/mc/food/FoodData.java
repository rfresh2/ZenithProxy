package com.zenith.mc.food;

import com.zenith.mc.RegistryData;
import com.zenith.mc.item.ItemRegistry;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ConsumeEffect;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;

import java.util.EnumSet;

public record FoodData(
    int id,
    String name,
    int stackSize,
    float foodPoints,
    float saturation,
    boolean canAlwaysEat,
    boolean isSafeFood
) implements RegistryData {

    // todo: cache result?
    public EnumSet<Effect> onConsumeEffects() {
        var set = EnumSet.noneOf(Effect.class);
        var itemData = ItemRegistry.REGISTRY.get(id);
        if (itemData == null) return set;
        var consumableComponent = itemData.components().get(DataComponentTypes.CONSUMABLE);
        if (consumableComponent == null) return set;
        if (consumableComponent.onConsumeEffects().isEmpty()) return set;
        for (var consumeEffect : consumableComponent.onConsumeEffects()) {
            if (consumeEffect instanceof ConsumeEffect.ApplyEffects applyEffects) {
                for (var mobEffectInstance : applyEffects.effects()) {
                    set.add(mobEffectInstance.getEffect());
                }
            }
        }
        return set;
    }
}
