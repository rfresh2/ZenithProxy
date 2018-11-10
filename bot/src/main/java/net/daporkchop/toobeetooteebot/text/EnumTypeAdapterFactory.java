/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2016-2018 DaPorkchop_
 *
 * Permission is hereby granted to any persons and/or organizations using this software to copy, modify, merge, publish, and distribute it.
 * Said persons and/or organizations are not allowed to use the software or any derivatives of the work for commercial use or any other means to generate income, nor are they allowed to claim this software as their own.
 *
 * The persons and/or organizations are also disallowed from sub-licensing and/or trademarking this software without explicit permission from DaPorkchop_.
 *
 * Any persons and/or organizations using this software must disclose their source code and have it publicly available, include this license, provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

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