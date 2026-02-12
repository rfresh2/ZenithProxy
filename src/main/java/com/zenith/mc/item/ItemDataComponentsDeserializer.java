package com.zenith.mc.item;

import com.google.common.primitives.Ints;
import io.netty.buffer.Unpooled;
import jodd.util.Base64;
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

            var id = Ints.tryParse(fieldName);
            var base64Value = p.getString();

            var dataBytes = Base64.decode(base64Value);
            var buf = Unpooled.wrappedBuffer(dataBytes);
            try {
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
