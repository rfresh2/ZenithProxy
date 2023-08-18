/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2018-2022 DaPorkchop_
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

package net.daporkchop.lib.minecraft.text.format;

import com.zenith.util.Color;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * All color codes from the legacy formatting system.
 * <p>
 * See https://minecraft.gamepedia.com/Formatting_codes for more information.
 *
 * @author DaPorkchop_
 * @see ChatFormat
 * @see FormattingCode
 */
@Getter
@Accessors(fluent = true)
public enum ChatColor implements FormattingCode {
    BLACK('0', new Color(0, 0, 0), new Color(0, 0, 0)),
    DARK_BLUE('1', 0x0000AA, 0x00002A),
    DARK_GREEN('2', 0x00AA00, 0x002A00),
    DARK_AQUA('3', 0x00AAAA, 0x002A2A),
    DARK_RED('4', 0xAA0000, 0x2A0000),
    DARK_PURPLE('5', 0xAA00AA, 0x2A002A),
    GOLD('6', 0xFFAA00, 0x2A2A00),
    GRAY('7', 0xAAAAAA, 0x2A2A2A),
    DARK_GRAY('8', 0x555555, 0x151515),
    BLUE('9', 0x5555FF, 0x15153F),
    GREEN('a', 0x55FF55, 0x153F15),
    AQUA('b', 0x55FFFF, 0x153F3F),
    RED('c', 0xFF5555, 0x3F1515),
    LIGHT_PURPLE('d', 0xFF55FF, 0x3F153F),
    YELLOW('e', 0xFFFF55, 0x3F3F15),
    WHITE('f', new Color(255, 255, 255), new Color(63, 63, 63));

    static final Pattern PATTERN = Pattern.compile("§[0-9a-fk-or]", Pattern.CASE_INSENSITIVE);

    static final ChatColor[] VALUES = values();

    static final FormattingCode[] CODE_LOOKUP = new FormattingCode['r' + 1];

    static final Map<String, FormattingCode> NAME_LOOKUP = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    static {
        for (ChatColor color : VALUES) {
            CODE_LOOKUP[Character.toLowerCase(color.code)] = color;
            CODE_LOOKUP[Character.toUpperCase(color.code)] = color;

            NAME_LOOKUP.put(color.name(), color);
        }
        for (ChatFormat format : ChatFormat.VALUES) {
            CODE_LOOKUP[Character.toLowerCase(format.code())] = format;
            CODE_LOOKUP[Character.toUpperCase(format.code())] = format;

            NAME_LOOKUP.put(format.name(), format);
        }
    }

    static ChatColor closestTo(final Color color, final int rgb) {
        ChatColor closest = null;
        int closestDist = 1 << 30;

        for (ChatColor format : VALUES) {
            if (color == format.colorInstance) {
                //if the colors match at an identity level, blindly accept it
                return format;
            }

            int vR = ((rgb >>> 16) & 0xFF) - ((format.color >>> 16) & 0xFF);
            int vG = ((rgb >>> 8) & 0xFF) - ((format.color >>> 8) & 0xFF);
            int vB = (rgb & 0xFF) - (format.color & 0xFF);
            int dist = vR * vR + vG * vG + vB * vB; //distanceSq between the two colors

            if (dist < closestDist) {
                closestDist = dist;
                closest = format;
            }
        }

        return closest;
    }

    public static ChatColor fromIndex(int index) {
        return VALUES[index];
    }

    /**
     * The in-game color of this formatting code, as an AWT {@link Color}.
     * <p>
     * If {@code null}, this formatting code has no color.
     *
     * @see #color
     */
    protected final Color colorInstance;

    /**
     * The in-game background color of this formatting code, as an AWT {@link Color}.
     * <p>
     * Used for drawing the text shadow in-game.
     * <p>
     * If {@code null}, this formatting code has no background color.
     *
     * @see #bgColor
     */
    protected final Color bgColorInstance;

    /**
     * The in-game ARGB color of this formatting code.
     * <p>
     * If not fully opaque (i.e. {@code (~color & 0xFF000000) != 0}, this formatting code has no color.
     */
    protected final int color;

    /**
     * The in-game ARGB background color of this formatting code.
     * <p>
     * Used for drawing the text shadow in-game.
     * <p>
     * If not fully opaque (i.e. {@code (~bgColor & 0xFF000000) != 0}, this formatting code has no background color.
     */
    protected final int bgColor;

    /**
     * The single-letter identifier for this formatting code.
     */
    protected final char code;

    ChatColor(char code, Color color, Color bgColor) {
        this.color = (this.colorInstance = color) != null ? (color.getRGB() | 0xFF000000) : 0;
        this.bgColor = (this.bgColorInstance = bgColor) != null ? (color.getRGB() | 0xFF000000) : 0;
        this.code = code;
    }

    ChatColor(char code, int color, int bgColor) {
        this(code, Color.fromInt(color), Color.fromInt(bgColor));
    }

    @Override
    public String toString() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean isColor() {
        return true;
    }
}
