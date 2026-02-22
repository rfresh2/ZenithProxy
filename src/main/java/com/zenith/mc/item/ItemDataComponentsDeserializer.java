package com.zenith.mc.item;

import com.google.common.primitives.Ints;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.util.HashMap;

class ItemDataComponentsDeserializer extends StdDeserializer<DataComponents> {
    protected ItemDataComponentsDeserializer() {
        super(DataComponents.class);
    }

    @Override
    public DataComponents deserialize(final JsonParser p, final DeserializationContext ctxt) throws JacksonException {
        if (!p.isExpectedStartObjectToken()) {
            throw ctxt.wrongTokenException(p, DataComponents.class, JsonToken.START_OBJECT, "Expected start of object for DataComponents");
        }

        var components = new DataComponents(new HashMap<>());
        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();
            if (p.currentToken() != JsonToken.VALUE_EMBEDDED_OBJECT && p.currentToken() != JsonToken.VALUE_STRING) {
                throw ctxt.wrongTokenException(p, byte[].class, JsonToken.VALUE_EMBEDDED_OBJECT, "Expected binary value for DataComponents entry");
            }

            var id = Ints.tryParse(fieldName);
            if (id == null) throw new RuntimeException("Missing DataComponent ID: " + fieldName);
            var buf = ByteBufAllocator.DEFAULT.buffer();
            try {
                p.readBinaryValue(new ByteBufOutputStream(buf));
                DataComponentType type = DataComponentTypes.from(id);
                DataComponent dataComponent = type.readDataComponent(buf);
                components.put(type, dataComponent.getValue());
            } finally {
                buf.release();
            }
        }
        return components;
    }
}
