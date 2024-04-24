package com.zenith.feature.map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.zenith.util.Color;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import static com.zenith.Shared.OBJECT_MAPPER;

public class MapBlockColorManager {
    private final Object2IntMap<String> blockNameToMapColorId = new Object2IntOpenHashMap<>(1058);
    // todo: provide alternative color map? https://github.com/Godlander/vpp/blob/main/assets/minecraft/shaders/core/render/text.fsh
    private final Int2IntMap mapColorIdToColor = new Int2IntOpenHashMap(64);

    public MapBlockColorManager() {
        blockNameToMapColorId.defaultReturnValue(0); // empty color id
        init();
    }

    public void init() {
        try (JsonParser blockColorsParser = OBJECT_MAPPER.createParser(getClass().getResourceAsStream("/mcdata/blockToMapColorId.json"))) {
            TreeNode node = blockColorsParser.getCodec().readTree(blockColorsParser);
            node.fieldNames().forEachRemaining((blockName) -> {
                var id = ((IntNode) node.get(blockName)).asInt();
                blockNameToMapColorId.put(blockName, id);
            });
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        try (JsonParser mapColorsParser = OBJECT_MAPPER.createParser(getClass().getResourceAsStream("/mcdata/mapColorIdToColor.json"))) {
            TreeNode node = mapColorsParser.getCodec().readTree(mapColorsParser);
            node.fieldNames().forEachRemaining((colorId) -> {
                var color = ((IntNode) node.get(colorId)).asInt();
                mapColorIdToColor.put(Integer.parseInt(colorId), color);
            });
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int getMapColorId(String blockName) {
        return blockNameToMapColorId.getInt(blockName);
    }

    public int getColor(final int mapColorId) {
        return mapColorIdToColor.get(mapColorId);
    }

    public int getColor(final String blockName) {
        return getColor(getMapColorId(blockName));
    }

    public Color calculateRGBColor(final int mapColor, final Brightness brightness) {
        if (mapColor == 0) {
            return Color.BLACK;
        } else {
            int i = brightness.modifier;
            int r = (mapColor >> 16 & 0xFF) * i / 255;
            int g = (mapColor >> 8 & 0xFF) * i / 255;
            int b = (mapColor & 0xFF) * i / 255;
            return new Color(r, g, b);
        }
    }

    public int calculateRGBColorI(final int mapColor, final Brightness brightness) {
        if (mapColor == 0) {
            return 0;
        } else {
            int i = brightness.modifier;
            int r = (mapColor >> 16 & 0xFF) * i / 255;
            int g = (mapColor >> 8 & 0xFF) * i / 255;
            int b = (mapColor & 0xFF) * i / 255;
            return 255 << 24 | r << 16 | g << 8 | b;
        }
    }

    public byte getPackedId(int mapColorId, Brightness brightness) {
        return (byte)(mapColorId << 2 | brightness.id & 3);
    }
}
