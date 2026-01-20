package com.zenith.plugin.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.primitives.Ints;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

@Data
public class Version implements Comparable<Version> {
    public static final String VERSION_PATTERN_STRING = "[0-9]+\\.[0-9]+\\.[0-9]+";
    public static final Pattern VERSION_PATTERN = Pattern.compile(VERSION_PATTERN_STRING);
    private final String version;
    private final int[] parts = new int[3];

    @JsonCreator
    public Version(String version) {
        if (!VERSION_PATTERN.matcher(version).matches()) {
            throw new IllegalArgumentException("Invalid version format: " + version);
        }
        this.version = version;
        String[] split = version.split("\\.");
        if (split.length != 3) {
            throw new IllegalArgumentException("Invalid version format: " + version);
        }
        for (int i = 0; i < 3; i++) {
            Integer parsed = Ints.tryParse(split[i]);
            if (parsed == null) {
                throw new IllegalArgumentException("Invalid version part: " + split[i]);
            }
            parts[i] = parsed;
        }
    }

    public Version(int major, int minor, int patch) {
        this.version = String.format("%d.%d.%d", major, minor, patch);
        this.parts[0] = major;
        this.parts[1] = minor;
        this.parts[2] = patch;
    }

    public int getMajor() {
        return parts[0];
    }

    public int getMinor() {
        return parts[1];
    }

    public int getPatch() {
        return parts[2];
    }

    @Override
    public int compareTo(@NotNull final Version o) {
        for (int i = 0; i < parts.length; i++) {
            if (parts[i] != o.parts[i]) {
                return Integer.compare(parts[i], o.parts[i]);
            }
        }
        return 0;
    }

    @JsonValue
    @Override
    public String toString() {
        return version;
    }

    public static boolean validate(String version) {
        return VERSION_PATTERN.matcher(version).matches();
    }
}
