package com.zenith.mc.item;

import com.zenith.mc.JsonRegistrySpec;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import jodd.util.Base64;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

import java.util.HashMap;

import static com.zenith.Globals.OBJECT_MAPPER;

public class ItemRegistrySpec implements JsonRegistrySpec<ItemData> {
    @Override
    public String filePath() {
        return "/mcdata/items.json";
    }

    @Override
    public Class<ItemData> dataClass() {
        return ItemData.class;
    }

    @Override
    public Int2ObjectMap<ItemData> read() {
        Int2ObjectMap<ItemData> map;
        try (var parser = OBJECT_MAPPER.createParser(JsonRegistrySpec.class.getResourceAsStream(filePath()))) {
            parser.nextToken();
            parser.nextToken();
            var dataList = OBJECT_MAPPER.readValues(parser, SerializedItemData.class).readAll();
            map = new Int2ObjectArrayMap<>(dataList.size());
            for (var data : dataList) {
                var convertedData = new ItemData(
                    data.id(),
                    data.name(),
                    data.stackSize(),
                    deserializeDataComponents(data.components()),
                    data.toolTag()
                );
                map.put(convertedData.id(), convertedData);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        return map;
    }

    private DataComponents deserializeDataComponents(Int2ObjectMap<String> serializedComponents) {
        var components = new DataComponents(new HashMap<>(serializedComponents.size()));
        for (var e : serializedComponents.int2ObjectEntrySet()) {
            // todo: maybe possible to pool a byte array? to avoid gc spam
            var dataBytes = Base64.decode(e.getValue());
            var buf = Unpooled.wrappedBuffer(dataBytes);
            try {
                DataComponentType type = DataComponentTypes.from(e.getIntKey());
                DataComponent dataComponent = type.readDataComponent(buf);
                components.put(type, dataComponent.getValue());
            } finally {
                buf.release();
            }
        }
        return components;
    }
}
