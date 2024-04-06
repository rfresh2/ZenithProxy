package com.zenith.util;

import discord4j.rest.util.Color;

public enum ConfigColor {
    WHITE(Color.WHITE),
    LIGHT_GRAY(Color.LIGHT_GRAY),
    GRAY(Color.GRAY),
    DARK_GRAY(Color.DARK_GRAY),
    BLACK(Color.BLACK),
    RED(Color.RED),
    PINK(Color.PINK),
    ORANGE(Color.ORANGE),
    YELLOW(Color.YELLOW),
    GREEN(Color.GREEN),
    MAGENTA(Color.MAGENTA),
    CYAN(Color.CYAN),
    BLUE(Color.BLUE),
    LIGHT_SEA_GREEN(Color.LIGHT_SEA_GREEN),
    MEDIUM_SEA_GREEN(Color.MEDIUM_SEA_GREEN),
    SUMMER_SKY(Color.SUMMER_SKY),
    DEEP_LILAC(Color.DEEP_LILAC),
    RUBY(Color.RUBY),
    MOON_YELLOW(Color.MOON_YELLOW),
    TAHITI_GOLD(Color.TAHITI_GOLD),
    CINNABAR(Color.CINNABAR),
    SUBMARINE(Color.SUBMARINE),
    HOKI(Color.HOKI),
    DEEP_SEA(Color.DEEP_SEA),
    SEA_GREEN(Color.SEA_GREEN),
    ENDEAVOUR(Color.ENDEAVOUR),
    VIVID_VIOLET(Color.VIVID_VIOLET),
    JAZZBERRY_JAM(Color.JAZZBERRY_JAM),
    DARK_GOLDENROD(Color.DARK_GOLDENROD),
    RUST(Color.RUST),
    BROWN(Color.BROWN),
    GRAY_CHATEAU(Color.GRAY_CHATEAU),
    BISMARK(Color.BISMARK);

    private final Color discordColor;

    ConfigColor(Color discordColor) {
        this.discordColor = discordColor;
    }

    public Color discord() {
        return discordColor;
    }
}
