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
import lombok.NonNull;

import static net.daporkchop.lib.minecraft.text.format.ChatColor.*;

/**
 * Base representation of a Minecraft text formatting code.
 *
 * @author DaPorkchop_
 * @see ChatFormat
 */
public interface FormattingCode {
    /**
     * Strips all formatting codes from the given input text.
     *
     * @param input the {@link CharSequence} to clean
     * @return the cleaned text
     */
    static String clean(@NonNull CharSequence input) {
        return PATTERN.matcher(input).replaceAll("");
    }

    /**
     * Gets the {@link ChatColor} whose color is most similar to the given {@link Color}.
     *
     * @param color the {@link Color} to find a match for
     * @return the {@link ChatColor} whose color is most similar to the given {@link Color}
     */
    static ChatColor closestTo(Color color) {
        return color == null ? null : ChatColor.closestTo(color, color.getRGB());
    }

    /**
     * Gets the {@link ChatColor} whose color is most similar to the given RGB color.
     *
     * @param rgb the RGB color to find a match for
     * @return the {@link ChatColor} whose color is most similar to the given color
     */
    static ChatColor closestTo(int rgb) {
        return ChatColor.closestTo(null, rgb);
    }

    /**
     * Finds the {@link ChatColor} with the given formatting code.
     *
     * @param code the formatting code to search for
     * @return the {@link ChatColor} with the given formatting code, or {@code null} if none could be found
     */
    static FormattingCode lookup(char code) {
        return code <= 'r' ? CODE_LOOKUP[code] : null;
    }

    /**
     * Finds the {@link ChatColor} with the given color (not formatting!) code.
     *
     * @param code the color code to search for
     * @return the {@link ChatColor} with the given color code, or {@code null} if none could be found
     */
    static ChatColor lookupColor(char code) {
        FormattingCode format = lookup(code);
        return format instanceof ChatColor ? (ChatColor) format : null;
    }

    /**
     * Finds the {@link ChatColor} with the given name.
     *
     * @param name the name of the formatting code to search for
     * @return the {@link ChatColor} with the given name, or {@code null} if none could be found
     */
    static FormattingCode lookup(@NonNull String name) {
        FormattingCode format;
        if (name.length() == 1 && (format = lookup(name.charAt(0))) != null) {
            return format;
        }
        return NAME_LOOKUP.get(name);
    }

    /**
     * Finds the {@link ChatColor} with the given name.
     *
     * @param name the name of the color code to search for
     * @return the {@link ChatColor} with the given name, or {@code null} if none could be found
     */
    static ChatColor lookupColor(@NonNull String name) {
        FormattingCode format = lookup(name);
        return format instanceof ChatColor ? (ChatColor) format : null;
    }

    /**
     * @return the 1-character code used to identify this formatting code
     */
    char code();

    /**
     * @return the lowercase name of this formatting code
     */
    @Override
    String toString();

    /**
     * @return whether or not this formatting code is a color
     */
    boolean isColor();
}
