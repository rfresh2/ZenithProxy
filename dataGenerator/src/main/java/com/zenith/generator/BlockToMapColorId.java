package com.zenith.generator;

import com.zenith.DataGenerator;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import static com.zenith.DataGenerator.LOG;

public class BlockToMapColorId implements Generator {
    public void generate() {
        var map = new Object2IntLinkedOpenHashMap<>();
        var blockRegistry = BuiltInRegistries.BLOCK;
        IdMap<Holder<Block>> holderIdMap = blockRegistry.asHolderIdMap();
        for (int id = 0; id < holderIdMap.size(); id++) {
            Holder<Block> blockHolder = holderIdMap.byId(id);
            ResourceKey<Block> blockResourceKey = blockHolder.unwrapKey().get();
            var block = blockHolder.value();
            var blockName = blockResourceKey.location().getPath();
            var mapColor = block.defaultMapColor();
            var mapColorId = mapColor.id;
            map.put(blockName, mapColorId);
        }
        try (Writer out = new FileWriter(DataGenerator.outputFile("blockToMapColorId.json"))) {
            DataGenerator.gson.toJson(map, out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LOG.info("Dumped blockToMapColorId.json");
    }
}
