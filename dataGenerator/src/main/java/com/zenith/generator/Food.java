package com.zenith.generator;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zenith.DataGenerator;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

import java.io.FileWriter;
import java.io.Writer;
import java.util.Objects;

public class Food implements Generator {
    @Override
    public void generate() {
        JsonArray foodArray = new JsonArray();
        DefaultedRegistry<Item> registry = BuiltInRegistries.ITEM;
        registry.stream()
            .filter(Item::isEdible)
            .forEach(food -> foodArray.add(generateFoodDescriptor(registry, food)));
        try (Writer out = new FileWriter(DataGenerator.outputFile("foods.json"))) {
            DataGenerator.gson.toJson(foodArray, out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        DataGenerator.LOG.info("Dumped foods.json");
    }


    public static JsonObject generateFoodDescriptor(Registry<Item> registry, Item foodItem) {
        JsonObject foodDesc = new JsonObject();
        var registryKey = registry.getKey(foodItem);

        foodDesc.addProperty("id", registry.getId(foodItem));
        foodDesc.addProperty("name", registryKey.getPath());

        foodDesc.addProperty("stackSize", foodItem.getMaxStackSize());
        var lang = net.minecraft.locale.Language.getInstance();
        foodDesc.addProperty("displayName", lang.getOrDefault(foodItem.getDescriptionId()));

        FoodProperties foodComponent = Objects.requireNonNull(foodItem.getFoodComponent(foodItem.getDefaultInstance()));
        float foodPoints = foodComponent.getNutrition();
        float saturationRatio = foodComponent.getSaturationModifier() * 2.0F;
        float saturation = foodPoints * saturationRatio;

        foodDesc.addProperty("foodPoints", foodPoints);
        foodDesc.addProperty("saturation", saturation);

        foodDesc.addProperty("effectiveQuality", foodPoints + saturation);
        foodDesc.addProperty("saturationRatio", saturationRatio);
        return foodDesc;
    }
}
