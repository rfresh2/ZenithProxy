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

package net.daporkchop.lib.minecraft.text.parser;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.daporkchop.lib.logging.format.FormatParser;
import net.daporkchop.lib.minecraft.text.component.MCTextRoot;
import net.daporkchop.lib.minecraft.text.util.TranslationSource;

/**
 * Parses Minecraft text into formatted components, automatically detecting Json or legacy-formatted text.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
@ToString
public final class AutoMCFormatParser implements FormatParser {
    public static final AutoMCFormatParser DEFAULT = new AutoMCFormatParser(JsonTextParser.DEFAULT, LegacyTextParser.INSTANCE);

    @NonNull
    protected final JsonTextParser jsonTextParser;
    @NonNull
    protected final LegacyTextParser legacyTextParser;

    public AutoMCFormatParser(@NonNull TranslationSource translationSource) {
        this(new JsonTextParser(translationSource), LegacyTextParser.INSTANCE);
    }

    @Override
    public MCTextRoot parse(@NonNull String text) {
        if (text.indexOf('§') == -1) { //simple test to detect legacy text
            try {
                //attempt to parse as json text
                return this.jsonTextParser.parse(JsonParser.parseString(text), text);
            } catch (JsonSyntaxException e) {
            }
        }
        //not a json string, treat as legacy
        return this.legacyTextParser.parse(text);
    }
}
