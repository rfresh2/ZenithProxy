package com.zenith.mc;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import static com.zenith.mc.MCGlobals.OBJECT_MAPPER;

public interface JsonRegistrySpec<T extends RegistryData> {
    String filePath();
    Class<T> dataClass();
    default Int2ObjectMap<T> read() {
        Int2ObjectMap<T> map;
        try (var parser = OBJECT_MAPPER.createParser(JsonRegistrySpec.class.getResourceAsStream(filePath()))) {
            parser.nextToken();
            parser.nextToken();
            var dataList = OBJECT_MAPPER.readValues(parser, dataClass()).readAll();
            map = new Int2ObjectArrayMap<>(dataList.size());
            for (T data : dataList) {
                map.put(data.id(), data);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        return map;
    }
}
