package net.daporkchop.toobeetooteebot.text;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

public class EnumTypeAdapterFactory implements TypeAdapterFactory {
    public <T> TypeAdapter<T> create(Gson p_create_1_, TypeToken<T> p_create_2_) {
        Class<T> oclass = (Class<T>) p_create_2_.getRawType();

        if (!oclass.isEnum()) {
            return null;
        } else {
            final Map<String, T> map = Maps.newHashMap();

            for (T t : oclass.getEnumConstants()) {
                map.put(this.getName(t), t);
            }

            return new TypeAdapter<T>() {
                public void write(JsonWriter p_write_1_, T p_write_2_) throws IOException {
                    if (p_write_2_ == null) {
                        p_write_1_.nullValue();
                    } else {
                        p_write_1_.value(EnumTypeAdapterFactory.this.getName(p_write_2_));
                    }
                }

                public T read(JsonReader p_read_1_) throws IOException {
                    if (p_read_1_.peek() == JsonToken.NULL) {
                        p_read_1_.nextNull();
                        return null;
                    } else {
                        return map.get(p_read_1_.nextString());
                    }
                }
            };
        }
    }

    private String getName(Object objectIn) {
        return objectIn instanceof Enum ? ((Enum) objectIn).name().toLowerCase(Locale.ROOT) : objectIn.toString().toLowerCase(Locale.ROOT);
    }
}