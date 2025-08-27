package com.zenith.plugin.api;

import java.io.Reader;
import java.io.Writer;

public interface ConfigSerializer {
    /**
     * Writes the Config instance to file
     */
    void write(Object config, Writer writer);

    /**
     * Reads the Config instance from file
     */
    <T> T read(Class<T> configClass, Reader reader);

    /**
     * The config's file extension, without the dot, defaults to "json"
     */
    default String fileExtension() { return "json"; }
}
