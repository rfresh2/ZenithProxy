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

package net.daporkchop.lib.logging.format.component;

import com.zenith.util.Color;
import lombok.NonNull;
import net.daporkchop.lib.logging.format.TextStyle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * Base component in a formatted text string.
 *
 * @author DaPorkchop_
 */
public interface TextComponent {
    /**
     * Gets this component's text (and the text of all children, if present).
     *
     * @return the raw text
     */
    default String toRawString() {
        StringBuilder builder = new StringBuilder(); //TODO: pool these
        this.internal_toRawStringRecursive(builder);
        return builder.toString();
    }

    /**
     * Gets this component's text.
     * <p>
     * This method returns {@code null} if it does not contain any text itself.
     *
     * @return this component's text
     */
    String getText();

    /**
     * Gets a list containing all child elements of this component.
     * <p>
     * This method may never return {@code null}, and should return an empty list (e.g. via {@link Collections#emptyList()}) if it does not contain any
     * children.
     *
     * @return an immutable list containing all children of this text component
     */
    List<TextComponent> getChildren();

    /**
     * Adds a new child element to this component.
     *
     * @param child the child to add
     */
    void pushChild(@NonNull TextComponent child);

    /**
     * Pops the last child element off of this component.
     *
     * @return the child that was removed
     */
    TextComponent popChild();

    /**
     * Gets this text component's color, if set. If no color is explicitly set (i.e. the default color should be used), this method returns {@code null}.
     *
     * @return this text component's color
     */
    Color getColor();

    /**
     * Gets this text component's background color, if set. If no color is explicitly set (i.e. the default background color should be used), this method
     * returns {@code null}.
     *
     * @return this text component's background color
     */
    Color getBackgroundColor();

    /**
     * Gets this text component's text style.
     *
     * @return this text component's text style
     * @see TextStyle
     */
    int getStyle();

    default void internal_toRawStringRecursive(@NonNull StringBuilder builder) {
        {
            String text = this.getText();
            if (text != null) {
                builder.append(text);
            }
        }
        for (TextComponent child : this.getChildren()) {
            child.internal_toRawStringRecursive(builder);
        }
    }

    //test stuff
    default boolean hasNewline() {
        if (this.getText() != null && this.getText().indexOf('\n') != -1) {
            return true;
        } else {
            for (TextComponent child : this.getChildren()) {
                if (child.hasNewline()) {
                    return true;
                }
            }
            return false;
        }
    }

    default Stream<TextComponent> splitOnNewlines() {
        List<TextComponent> cache = new ArrayList<>();
        AtomicReference<TextComponent> ref = new AtomicReference<>(new TextComponentHolder());
        this.internal_addComponents(cache, ref, this, null);
        if (!ref.get().getChildren().isEmpty()) {
            cache.add(ref.get());
        }
        return cache.stream();
    }

    default void internal_addComponents(@NonNull List<TextComponent> cache, @NonNull AtomicReference<TextComponent> curr, @NonNull TextComponent component, TextComponent parent) {
        {
            String text = component.getText();
            if (text != null && !text.isEmpty()) {
                if (text.indexOf('\n') == -1) {
                    curr.get().pushChild(component);
                } else {
                    int newlineCount = 0;
                    for (int i = text.length() - 1; i >= 0; i--) {
                        if (text.charAt(i) == '\n') {
                            newlineCount++;
                        }
                    }
                    String[] split = text.split("\n");
                    for (String line : split) {
                        curr.get().pushChild(new TextComponentString(line, component.getColor(), component.getBackgroundColor(), component.getStyle()));
                        if (newlineCount-- <= 0) {
                            continue;
                        }
                        cache.add(curr.getAndSet(new TextComponentHolder()));
                    }
                }
            }
        }
        for (TextComponent child : component.getChildren()) {
            this.internal_addComponents(cache, curr, child, component);
        }
    }
}
