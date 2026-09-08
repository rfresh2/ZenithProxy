package com.zenith.plugin.api;

import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Data from each plugin's {@link Plugin} annotation.
 */
@NullMarked
public record PluginInfo(
    String entrypoint,
    String id,
    Version version,
    String description,
    String url,
    List<String> authors,
    List<String> mcVersions,
    List<String> mixins
) {
    public PluginInfo {
        // will occur if a legacy plugin is loaded, they don't have a mixins field
        if (mixins == null) {
            mixins = List.of();
        }
    }

    public static final String ID_PATTERN_STRING = "[a-z][a-z0-9-_]{0,63}";
    public static final Pattern ID_PATTERN = Pattern.compile(ID_PATTERN_STRING);
}
