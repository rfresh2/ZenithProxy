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

package net.daporkchop.lib.minecraft.text.parser;

import com.zenith.util.Color;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.daporkchop.lib.logging.console.TextFormat;
import net.daporkchop.lib.logging.format.TextStyle;
import net.daporkchop.lib.logging.format.component.TextComponentString;
import net.daporkchop.lib.minecraft.text.MCTextType;
import net.daporkchop.lib.minecraft.text.component.MCTextRoot;
import net.daporkchop.lib.minecraft.text.format.ChatColor;
import net.daporkchop.lib.minecraft.text.format.FormattingCode;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import static net.daporkchop.lib.minecraft.text.format.ChatFormat.*;

/**
 * @author DaPorkchop_
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LegacyTextParser implements MCFormatParser {
    public static final LegacyTextParser INSTANCE = new LegacyTextParser();

    protected static MCTextRoot parse(@NonNull Reader reader, String raw) throws IOException {
        MCTextRoot root = new MCTextRoot(MCTextType.LEGACY, raw);

        TextFormat format = new TextFormat();
        boolean expectingCode = false;

        StringBuilder builder = new StringBuilder();

        int nextChar;
        while ((nextChar = reader.read()) != -1) {
            if (expectingCode) {
                FormattingCode code = FormattingCode.lookup((char) nextChar);
                if (code == null) {
                    throw new IllegalArgumentException(String.format("Invalid formatting code: %c", (char) nextChar));
                }

                if (code.isColor()) {
                    format.setTextColor(Color.fromInt(((ChatColor) code).colorInstance().getRGB())).setStyle(0);
                } else {
                    if (code == BOLD) {
                        format.setStyle(format.getStyle() | TextStyle.BOLD);
                    } else if (code == STRIKETHROUGH) {
                        format.setStyle(format.getStyle() | TextStyle.STRIKETHROUGH);
                    } else if (code == UNDERLINE) {
                        format.setStyle(format.getStyle() | TextStyle.UNDERLINE);
                    } else if (code == ITALIC) {
                        format.setStyle(format.getStyle() | TextStyle.ITALIC);
                    } else if (code == RESET) {
                        format.setStyle(0).setTextColor(null);
                    }
                }
                expectingCode = false;
            } else if (nextChar == '§') {
                createComponent(root, builder, format);
                expectingCode = true;
            } else {
                builder.append((char) nextChar);
            }
        }
        createComponent(root, builder, format);
        return root;
    }

    protected static void createComponent(@NonNull MCTextRoot root, @NonNull StringBuilder builder, @NonNull TextFormat format) {
        if (builder.length() > 0) {
            root.pushChild(new TextComponentString(builder.toString(), format));
            builder.setLength(0);
        }
    }

    @Override
    public MCTextRoot parse(@NonNull String raw) {
        try {
            return parse(new StringReader(raw), raw);
        } catch (IOException e) {
            //impossible
            throw new IllegalStateException(e);
        }
    }

    @Override
    public MCTextType type() {
        return MCTextType.JSON;
    }
}
