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

package net.daporkchop.lib.logging.format.component;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public abstract class AbstractTextComponent implements TextComponent {
    protected final Color color;
    protected final Color backgroundColor;
    protected final int style;

    protected List<TextComponent> children = Collections.emptyList();

    public AbstractTextComponent()  {
        this(null, null, 0);
    }

    @Override
    public List<TextComponent> getChildren()    {
        List<TextComponent> children = this.children;
        return children == Collections.<TextComponent>emptyList() ? children : Collections.unmodifiableList(this.children);
    }

    @Override
    public synchronized void pushChild(@NonNull TextComponent child) {
        List<TextComponent> children = this.children;
        if (children == Collections.<TextComponent>emptyList()) {
            this.children = children = new ArrayList<>();
        }
        children.add(child);
    }

    @Override
    public synchronized TextComponent popChild() {
        List<TextComponent> children = this.children;
        if (children.isEmpty()) throw new IllegalStateException("stack underflow");
        return children.remove(children.size() - 1);
    }
}
