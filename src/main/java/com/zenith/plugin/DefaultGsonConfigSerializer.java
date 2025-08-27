package com.zenith.plugin;

import com.zenith.plugin.api.ConfigSerializer;

import java.io.Reader;
import java.io.Writer;

import static com.zenith.Globals.GSON;

public class DefaultGsonConfigSerializer implements ConfigSerializer {
    public static final DefaultGsonConfigSerializer INSTANCE = new DefaultGsonConfigSerializer();
    private DefaultGsonConfigSerializer() {}

    @Override
    public void write(final Object config, final Writer writer) {
        GSON.toJson(config, writer);
    }

    @Override
    public <T> T read(final Class<T> configClass, final Reader reader) {
        return GSON.fromJson(reader, configClass);
    }
}
