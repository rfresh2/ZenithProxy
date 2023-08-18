/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2018-2021 DaPorkchop_
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

package net.daporkchop.lib.minecraft.text.parser;

import com.google.gson.*;
import com.zenith.util.Color;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.logging.console.TextFormat;
import net.daporkchop.lib.logging.format.TextStyle;
import net.daporkchop.lib.logging.format.component.TextComponentString;
import net.daporkchop.lib.minecraft.text.MCTextType;
import net.daporkchop.lib.minecraft.text.component.MCTextRoot;
import net.daporkchop.lib.minecraft.text.format.ChatColor;
import net.daporkchop.lib.minecraft.text.format.FormattingCode;
import net.daporkchop.lib.minecraft.text.util.TranslationSource;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class JsonTextParser implements MCFormatParser {
    protected static final Pattern PATTERN = Pattern.compile("%(?:%|(?:(\\d+)\\$)?s)");

    public static final JsonTextParser DEFAULT = new JsonTextParser(TranslationSource.NONE);

    protected static void checkStyle(@NonNull JsonObject object, @NonNull TextFormat format, int mask, @NonNull String name) {
        JsonElement element = object.get(name);
        if (element != null) { //only update style if the json entry is present
            if (element.getAsBoolean()) {
                format.setStyle(format.getStyle() | mask);
            } else {
                format.setStyle(format.getStyle() & (~mask));
            }
        }
    }

    protected static void checkColor(@NonNull JsonObject object, @NonNull TextFormat format) {
        JsonElement element = object.get("color");
        if (element != null) { //only update color if the json entry is present
            String name = element.getAsString();
            FormattingCode formattingCode = FormattingCode.lookupColor(name);
            if (format == null) {
                if ("reset".equalsIgnoreCase(name)) {
                    format.setTextColor(null);
                } else {
                    throw new IllegalArgumentException("Unknown color code: \"" + name + '"');
                }
            } else if (!formattingCode.isColor()) {
                throw new IllegalStateException();
            } else {
                format.setTextColor(Color.fromInt(((ChatColor) formattingCode).colorInstance().getRGB()));
            }
        }
    }

    @NonNull
    protected final TranslationSource translationSource;

    @Override
    public MCTextRoot parse(@NonNull String raw) {
        try {
            return this.parse(JsonParser.parseString(raw), raw);
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("Invalid JSON!");
        }
    }

    public MCTextRoot parse(@NonNull JsonElement json, @NonNull String original) {
        MCTextRoot root = new MCTextRoot(MCTextType.JSON, original);
        this.doParseJson(root, new TextFormat(), json);
        return root;
    }

    @Override
    public MCTextType type() {
        return MCTextType.JSON;
    }

    protected void doParseJson(@NonNull MCTextRoot root, @NonNull TextFormat format, @NonNull JsonElement element) {
        String text = null;
        if (element.isJsonPrimitive()) {
            text = element.getAsString();
        } else if (element.isJsonArray()) {
            TextFormat childFormat = new TextFormat(); //re-use this instance to reduce allocations
            for (JsonElement child : element.getAsJsonArray()) {
                this.doParseJson(root, childFormat.copyFrom(format), child);
            }
            return;
        } else if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            checkStyle(object, format, TextStyle.BOLD, "bold");
            checkStyle(object, format, TextStyle.ITALIC, "italic");
            checkStyle(object, format, TextStyle.UNDERLINE, "underline");
            checkStyle(object, format, TextStyle.STRIKETHROUGH, "strikethrough");
            checkColor(object, format);

            JsonElement textElement;
            if ((textElement = object.get("text")) != null && textElement.isJsonPrimitive()) {
                text = textElement.getAsString();
            } else if ((textElement = object.get("translate")) != null && textElement.isJsonPrimitive()) {
                JsonElement with = object.get("with");
                text = this.appendTranslation(root, this.translationSource.translate(textElement.getAsString()), format, with.isJsonArray() ? with.getAsJsonArray() : new JsonArray());
            }
        }
        if (text != null && !text.isEmpty()) {
            root.pushChild(new TextComponentString(text, format));
        }

        if (element.isJsonObject()) {
            JsonElement extra = element.getAsJsonObject().get("extra");
            if (extra != null && extra.isJsonArray()) {
                this.doParseJson(root, format, extra);
            }
        }
    }

    protected String appendTranslation(@NonNull MCTextRoot root, @NonNull String translation, @NonNull TextFormat format, @NonNull JsonArray with) {
        TextFormat childFormat = new TextFormat();
        Matcher matcher = PATTERN.matcher(translation);
        StringBuffer rawBuffer = new StringBuffer();
        int nextFormatIndex = 0;

        while (matcher.find()) {
            if (translation.charAt(matcher.end() - 1) == '%') { //matched text is %%, append single % and continue without adding a component
                matcher.appendReplacement(rawBuffer, "%");
                continue;
            }
            matcher.appendReplacement(rawBuffer, "");

            String formatIndexText = matcher.group(1);
            int formatIndex = formatIndexText != null ? Integer.parseUnsignedInt(formatIndexText) - 1 : nextFormatIndex++;

            if (formatIndex < with.size()) { //format index is valid
                if (rawBuffer.length() != 0) { //add a new raw component if necessary
                    root.pushChild(new TextComponentString(rawBuffer.toString(), format));
                    rawBuffer.setLength(0);
                }
                this.doParseJson(root, childFormat.copyFrom(format), with.get(formatIndex));
            } //if format index is invalid, vanilla simply discards it and continues
        }
        matcher.appendTail(rawBuffer);

        return rawBuffer.length() != 0 ? rawBuffer.toString() : null;
    }
}
