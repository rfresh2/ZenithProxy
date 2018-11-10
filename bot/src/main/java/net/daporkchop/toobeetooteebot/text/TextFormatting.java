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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public enum TextFormatting {
    BLACK("BLACK", '0', 0),
    DARK_BLUE("DARK_BLUE", '1', 1),
    DARK_GREEN("DARK_GREEN", '2', 2),
    DARK_AQUA("DARK_AQUA", '3', 3),
    DARK_RED("DARK_RED", '4', 4),
    DARK_PURPLE("DARK_PURPLE", '5', 5),
    GOLD("GOLD", '6', 6),
    GRAY("GRAY", '7', 7),
    DARK_GRAY("DARK_GRAY", '8', 8),
    BLUE("BLUE", '9', 9),
    GREEN("GREEN", 'a', 10),
    AQUA("AQUA", 'b', 11),
    RED("RED", 'c', 12),
    LIGHT_PURPLE("LIGHT_PURPLE", 'd', 13),
    YELLOW("YELLOW", 'e', 14),
    WHITE("WHITE", 'f', 15),
    OBFUSCATED("OBFUSCATED", 'k', true),
    BOLD("BOLD", 'l', true),
    STRIKETHROUGH("STRIKETHROUGH", 'm', true),
    UNDERLINE("UNDERLINE", 'n', true),
    ITALIC("ITALIC", 'o', true),
    RESET("RESET", 'r', -1);

    /**
     * Maps a name (e.g., 'underline') to its corresponding enum value (e.g., UNDERLINE).
     */
    private static final Map<String, TextFormatting> NAME_MAPPING = Maps.newHashMap();
    /**
     * Matches formatting codes that indicate that the client should treat the following text as bold, recolored,
     * obfuscated, etc.
     */
    private static final Pattern FORMATTING_CODE_PATTERN = Pattern.compile("(?i)\u00a7[0-9A-FK-OR]");

    static {
        for (TextFormatting textformatting : values()) {
            NAME_MAPPING.put(lowercaseAlpha(textformatting.name), textformatting);
        }
    }

    /**
     * The name of this color/formatting
     */
    private final String name;
    private final boolean fancyStyling;
    /**
     * The control string (section sign + formatting code) that can be inserted into client-side text to display
     * subsequent text in this format.
     */
    private final String controlString;
    /**
     * The numerical index that represents this color
     */
    private final int colorIndex;

    TextFormatting(String formattingName, char formattingCodeIn, int colorIndex) {
        this(formattingName, formattingCodeIn, false, colorIndex);
    }

    TextFormatting(String formattingName, char formattingCodeIn, boolean fancyStylingIn) {
        this(formattingName, formattingCodeIn, fancyStylingIn, -1);
    }

    TextFormatting(String formattingName, char formattingCodeIn, boolean fancyStylingIn, int colorIndex) {
        this.name = formattingName;
        /*
      The formatting code that produces this format.
     */ /**
         * The formatting code that produces this format.
         */char formattingCode = formattingCodeIn;
        this.fancyStyling = fancyStylingIn;
        this.colorIndex = colorIndex;
        this.controlString = "\u00a7" + formattingCodeIn;
    }

    private static String lowercaseAlpha(String p_175745_0_) {
        return p_175745_0_.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
    }

    /**
     * Returns a copy of the given string, with formatting codes stripped away.
     */
    public static String getTextWithoutFormattingCodes(String text) {
        return text == null ? null : FORMATTING_CODE_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * Gets a value by its friendly name; null if the given name does not map to a defined value.
     */
    public static TextFormatting getValueByName(String friendlyName) {
        return friendlyName == null ? null : NAME_MAPPING.get(lowercaseAlpha(friendlyName));
    }

    /**
     * Get a TextFormatting from it's color index
     */
    public static TextFormatting fromColorIndex(int index) {
        if (index < 0) {
            return RESET;
        } else {
            for (TextFormatting textformatting : values()) {
                if (textformatting.getColorIndex() == index) {
                    return textformatting;
                }
            }

            return null;
        }
    }

    /**
     * Gets all the valid values.
     */
    public static Collection<String> getValidValues(boolean p_96296_0_, boolean p_96296_1_) {
        List<String> list = Lists.newArrayList();

        for (TextFormatting textformatting : values()) {
            if ((!textformatting.isColor() || p_96296_0_) && (!textformatting.isFancyStyling() || p_96296_1_)) {
                list.add(textformatting.getFriendlyName());
            }
        }

        return list;
    }

    /**
     * Returns the numerical color index that represents this formatting
     */
    public int getColorIndex() {
        return this.colorIndex;
    }

    /**
     * False if this is just changing the color or resetting; true otherwise.
     */
    public boolean isFancyStyling() {
        return this.fancyStyling;
    }

    /**
     * Checks if this is a color code.
     */
    public boolean isColor() {
        return !this.fancyStyling && this != RESET;
    }

    /**
     * Gets the friendly name of this value.
     */
    public String getFriendlyName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public String toString() {
        return this.controlString;
    }
}