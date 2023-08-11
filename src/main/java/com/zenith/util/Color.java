package com.zenith.util;

public record Color(int r, int g, int b) {
    public int getRGB() {
        return ((255 & 0xFF) << 24) |
            ((r & 0xFF) << 16) |
            ((g & 0xFF) << 8)  |
            ((b & 0xFF) << 0);
    }

    public static Color fromInt(int rgb) {
        return new Color(
            (rgb >> 16) & 0xFF,
            (rgb >> 8) & 0xFF,
            (rgb >> 0) & 0xFF
        );
    }
}
