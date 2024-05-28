package com.zenith.mc.map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.zenith.feature.map.Brightness;
import com.zenith.util.Color;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import static com.zenith.Shared.OBJECT_MAPPER;

public class MapBlockColorManager {
    // todo: provide alternative color map? https://github.com/Godlander/vpp/blob/main/assets/minecraft/shaders/core/render/text.fsh
    private final Int2IntMap mapColorIdToColor = new Int2IntOpenHashMap(64);

    public MapBlockColorManager() {
        init();
    }

    public void init() {
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

    public int getColor(final int mapColorId) {
        return mapColorIdToColor.get(mapColorId);
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
