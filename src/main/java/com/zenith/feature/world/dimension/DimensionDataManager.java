package com.zenith.feature.world.dimension;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Iterator;
import java.util.List;

import static com.zenith.Shared.OBJECT_MAPPER;

public class DimensionDataManager {
    private final Int2ObjectOpenHashMap<DimensionData> dimensionIdToData = new Int2ObjectOpenHashMap<>(4);
    private final Object2ObjectOpenHashMap<String, DimensionData> dimensionNameToData = new Object2ObjectOpenHashMap<>(4);

    public DimensionDataManager() {
        init();
    }

    private void init() {
        try (JsonParser dimensionParser = OBJECT_MAPPER.createParser(getClass().getResourceAsStream("/mcdata/dimensions.json"))) {
            TreeNode node = dimensionParser.getCodec().readTree(dimensionParser);
            for (Iterator<JsonNode> it = ((ArrayNode) node).elements(); it.hasNext(); ) {
                final var e = it.next();
                int id = e.get("id").asInt();
                String name = e.get("name").asText();
                int minY = e.get("minY").asInt();
                int buildHeight = e.get("buildHeight").asInt();
                int height = e.get("height").asInt();
                var dimensionData = new DimensionData(id, name, minY, buildHeight, height);
                dimensionIdToData.put(id, dimensionData);
                dimensionNameToData.put(name, dimensionData);
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public DimensionData getDimensionData(final int id) {
        var data = dimensionIdToData.get(id);
        if (data == dimensionIdToData.defaultReturnValue()) return null;
        return data;
    }

    public DimensionData getDimensionData(final String name) {
        var data = dimensionNameToData.get(name);
        if (data == dimensionNameToData.defaultReturnValue()) return null;
        return data;
    }

    public List<String> dimensionNames() {
        return List.copyOf(dimensionNameToData.keySet());
    }
}
