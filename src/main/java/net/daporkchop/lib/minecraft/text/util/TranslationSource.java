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

package net.daporkchop.lib.minecraft.text.util;

import lombok.NonNull;

import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * A source for translation keys.
 * <p>
 * This is effectively a {@code Function<String, String>}, allowing translations to be obtained from an arbitrary source (such as a {@link Map} or
 * {@link ResourceBundle}).
 *
 * @author DaPorkchop_
 */
@FunctionalInterface
public interface TranslationSource {
    /**
     * Default implementation of {@link TranslationSource}, always returns the input key.
     */
    TranslationSource NONE = key -> Objects.requireNonNull(key, "key");

    /**
     * Creates a {@link TranslationSource} as a view of the given {@link Map}.
     *
     * @param map the delegate {@link Map} to use
     * @return the newly created {@link TranslationSource}
     */
    static TranslationSource ofMap(@NonNull Map<String, String> map) {
        return key -> map.getOrDefault(Objects.requireNonNull(key, "key"), key);
    }

    /**
     * Gets the translation format for the given translation key.
     * <p>
     * If there is no known translation for the given key, implementations are generally expected to return the translation key itself.
     *
     * @param key the translation key to look up
     * @return the translation format
     */
    String translate(@NonNull String key);
}
