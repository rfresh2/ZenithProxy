package com.zenith.generator;

import com.mojang.datafixers.util.Pair;
import com.squareup.javapoet.CodeBlock;
import com.zenith.mc.food.FoodData;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ChorusFruitItem;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FoodRegistryGenerator extends RegistryGenerator<FoodData> {
    public FoodRegistryGenerator() {
        super(FoodData.class, FoodData.class.getPackage().getName(), "FoodRegistry");
    }

    @Override
    public List<FoodData> buildDataList() {
        List<FoodData> foodList = new ArrayList<>();

        DefaultedRegistry<Item> registry = BuiltInRegistries.ITEM;
        registry.stream()
            .filter(Item::isEdible)
            .forEach(food -> {
                FoodProperties foodComponent = Objects.requireNonNull(food.getFoodComponent(food.getDefaultInstance()));
                int foodPoints = foodComponent.getNutrition();
                float saturationRatio = foodComponent.getSaturationModifier() * 2.0F;
                float saturation = foodPoints * saturationRatio;
                List<MobEffect> effects = foodComponent.getEffects().stream()
                    .map(Pair::getFirst)
                    .map(MobEffectInstance::getEffect)
                    .toList();
                boolean isSafeFood = !effects.contains(MobEffects.POISON)
                    && !effects.contains(MobEffects.HUNGER)
                    && !(food instanceof ChorusFruitItem); // technically safe but better to avoid unexpected teleports
                var data = new FoodData(
                    registry.getId(food),
                    registry.getKey(food).getPath(),
                    food.getMaxStackSize(),
                    foodPoints,
                    saturation,
                    isSafeFood
                );
                foodList.add(data);
            });
        return foodList;
    }

    @Override
    public String dataNameMapper(final FoodData data) {
        return data.name();
    }

    @Override
    public int idMapper(final FoodData data) {
        return data.id();
    }

    @Override
    public CodeBlock dataInitializer(final FoodData data) {
        return CodeBlock.of(
            "new $T($L, $S, $L, $Lf, $Lf, $L)",
            FoodData.class,
            data.id(),
            data.name(),
            data.stackSize(),
            data.foodPoints(),
            data.saturation(),
            data.isSafeFood()
        );
    }
}
