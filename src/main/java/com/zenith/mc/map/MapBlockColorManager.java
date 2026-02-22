package com.zenith.mc.map;

import com.zenith.feature.map.Brightness;
import com.zenith.util.Color;
import com.zenith.util.struct.Maps;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import static com.zenith.mc.MCGlobals.OBJECT_MAPPER;

public class MapBlockColorManager {
    // todo: provide alternative color map? https://github.com/Godlander/vpp/blob/main/assets/minecraft/shaders/core/render/text.fsh
    private static final Int2IntMap mapColorIdToColor = new Int2IntOpenHashMap(64, Maps.FAST_LOAD_FACTOR);

    static {
        init();
    }

    private static void init() {
        var tree = OBJECT_MAPPER.readTree(MapBlockColorManager.class.getResourceAsStream("/mcdata/mapColorIdToColor.smile"));
        tree.propertyNames().forEach((colorId) -> {
            var color = tree.get(colorId).asInt();
            mapColorIdToColor.put(Integer.parseInt(colorId), color);
        });
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
