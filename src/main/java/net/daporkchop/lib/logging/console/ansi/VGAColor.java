/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2018-2020 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.lib.logging.console.ansi;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.awt.*;
import java.util.Arrays;

/**
 * The different colors that can be displayed in a VGA console.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public enum VGAColor {
    BLACK(30, 40, new Color(0, 0, 0)),
    RED(31, 41, new Color(170, 0, 0)),
    GREEN(32, 42, new Color(0, 170, 0)),
    YELLOW(33, 43, new Color(170, 170, 0)),
    BLUE(34, 44, new Color(0, 0, 170)),
    MAGENTA(35, 45, new Color(170, 0, 170)),
    CYAN(36, 46, new Color(0, 170, 170)),
    WHITE(37, 47, new Color(170, 170, 170)),
    BRIGHT_BLACK(90, 100, new Color(85, 85, 85)),
    BRIGHT_RED(91, 101, new Color(255, 85, 85)),
    BRIGHT_GREEN(92, 102, new Color(85, 255, 85)),
    BRIGHT_YELLOW(93, 103, new Color(255, 255, 85)),
    BRIGHT_BLUE(94, 104, new Color(85, 85, 255)),
    BRIGHT_MAGENTA(95, 105, new Color(255, 85, 255)),
    BRIGHT_CYAN(96, 106, new Color(85, 255, 255)),
    BRIGHT_WHITE(97, 107, new Color(255, 255, 255)),
    DEFAULT(39, 49, -1);

    protected static final VGAColor[] DISTANCE_SEARCH_VALUES = Arrays.copyOf(values(), DEFAULT.ordinal());

    public static VGAColor closestTo(Color color) {
        return color == null ? DEFAULT : closestTo(color.getRGB());
    }

    public static VGAColor closestTo(int val) {
        int r = (val >>> 16) & 0xFF;
        int g = (val >>> 8) & 0xFF;
        int b = val & 0xFF;

        VGAColor closest = null;
        val = Integer.MAX_VALUE; //reuse old variable for speed and stuff lol
        for (VGAColor color : DISTANCE_SEARCH_VALUES) {
            int vR = r - ((color.color >>> 16) & 0xFF);
            int vG = g - ((color.color >>> 8) & 0xFF);
            int vB = b - (color.color & 0xFF);
            int dist = vR * vR + vG * vG + vB * vB;
            if (dist == 0) {
                return color;
            } else if (dist < val) {
                val = dist;
                closest = color;
            }
        }
        return closest;
    }

    protected final int fg;
    protected final int bg;
    protected final int color;

    VGAColor(int fg, int bg, @NonNull Color color) {
        this(fg, bg, color.getRGB() & 0xFFFFFF);
    }
}
