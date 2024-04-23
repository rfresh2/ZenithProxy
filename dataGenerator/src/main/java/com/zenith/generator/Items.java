package com.zenith.generator;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zenith.DataGenerator;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.world.item.Item;

import java.io.FileWriter;
import java.io.Writer;

public class Items implements Generator {
    @Override
    public void generate() {
        JsonArray itemsArray = new JsonArray();
        DefaultedRegistry<Item> registry = BuiltInRegistries.ITEM;
        registry.stream().forEach(item -> itemsArray.add(generateItemDescriptor(registry, item)));
        try (Writer out = new FileWriter(DataGenerator.outputFile("items.json"))) {
            DataGenerator.gson.toJson(itemsArray, out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        DataGenerator.LOG.info("Dumped items.json");
    }

    private JsonObject generateItemDescriptor(final Registry<Item> registry, final Item item) {
        JsonObject itemDesc = new JsonObject();
        var registryKey = registry.getKey(item);

        itemDesc.addProperty("id", registry.getId(item));
        itemDesc.addProperty("name", registryKey.getPath());

        itemDesc.addProperty("displayName", Language.getInstance().getOrDefault(item.getDescriptionId()));
        itemDesc.addProperty("stackSize", item.getMaxStackSize());

//        List<Enchantment> enchantmentTargets = BuiltInRegistries.ENCHANTMENT.stream()
//            .filter(enchantment -> enchantment.canEnchant(item.getDefaultInstance()))
//            .toList();
//
//        JsonArray enchantCategoriesArray = new JsonArray();
//        for (Enchantment target : enchantmentTargets) {
//            enchantCategoriesArray.add(EnchantmentsDataGenerator.getEnchantmentTargetName(target));
//        }
//        itemDesc.add("enchantCategories", enchantCategoriesArray);

        if (item.canBeDepleted()) {
            int maxDurability = item.getMaxDamage();
            itemDesc.addProperty("maxDurability", maxDurability);
        }
        return itemDesc;
    }
}
