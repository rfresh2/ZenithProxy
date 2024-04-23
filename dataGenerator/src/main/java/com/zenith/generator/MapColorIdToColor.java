package com.zenith.generator;

import com.zenith.DataGenerator;
import it.unimi.dsi.fastutil.ints.Int2IntLinkedOpenHashMap;
import net.minecraft.world.level.material.MapColor;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import static com.zenith.DataGenerator.LOG;

public class MapColorIdToColor implements Generator {
    @Override
    public void generate() {
        var map = new Int2IntLinkedOpenHashMap();
        var colors = MapColor.MATERIAL_COLORS;
        for (int i = 0; i < colors.length; i++) {
            var color = colors[i];
            if (color != null) {
                map.put(i, color.col);
            } else {
                map.put(i, 0);
            }
        }
        try (Writer out = new FileWriter(DataGenerator.outputFile("mapColorIdToColor.json"))) {
            DataGenerator.gson.toJson(map, out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LOG.info("Dumped mapColorIdToColor.json");
    }
}
