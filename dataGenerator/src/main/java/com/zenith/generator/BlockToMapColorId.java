package com.zenith.generator;

import com.zenith.DataGenerator;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import static com.zenith.DataGenerator.LOG;

public class BlockToMapColorId implements Generator {
    public void generate() {
        var map = new Object2IntLinkedOpenHashMap<>();
        var blockRegistry = BuiltInRegistries.BLOCK;
        blockRegistry.entrySet().forEach((entry) -> {
            var block = entry.getValue();
            var id = entry.getKey();
            var blockName = id.location().getPath();
            var mapColor = block.defaultMapColor();
            var mapColorId = mapColor.id;
            map.put(blockName, mapColorId);
        });
        try (Writer out = new FileWriter(DataGenerator.outputFile("blockToMapColorId.json"))) {
            DataGenerator.gson.toJson(map, out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LOG.info("Dumped blockToMapColorId.json");
    }
}
