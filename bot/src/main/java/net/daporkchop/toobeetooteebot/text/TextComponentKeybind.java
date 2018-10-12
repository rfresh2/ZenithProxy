/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2016-2017 DaPorkchop_
 *
 * Permission is hereby granted to any persons and/or organizations using this software to copy, modify, merge, publish, and distribute it.
 * Said persons and/or organizations are not allowed to use the software or any derivatives of the work for commercial use or any other means to generate income, nor are they allowed to claim this software as their own.
 *
 * The persons and/or organizations are also disallowed from sub-licensing and/or trademarking this software without explicit permission from DaPorkchop_.
 *
 * Any persons and/or organizations using this software must disclose their source code and have it publicly available, include this license, provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.daporkchop.toobeetooteebot.text;

import java.util.function.Function;
import java.util.function.Supplier;

public class TextComponentKeybind extends TextComponentBase {
    public static Function<String, Supplier<String>> displaySupplierFunction = (p_193635_0_) ->
    {
        return () -> {
            return p_193635_0_;
        };
    };
    private final String keybind;
    private Supplier<String> displaySupplier;

    public TextComponentKeybind(String keybind) {
        super();
        this.keybind = keybind;
    }

    /**
     * Gets the raw content of this component (but not its sibling components), without any formatting codes. For
     * example, this is the raw text in a {@link TextComponentString}, but it's the translated text for a {@link
     * TextComponentTranslation} and it's the score value for a {@link TextComponentScore}.
     */
    public String getUnformattedComponentText() {
        if (this.displaySupplier == null) {
            this.displaySupplier = displaySupplierFunction.apply(this.keybind);
        }

        return this.displaySupplier.get();
    }

    /**
     * Creates a copy of this component.  Almost a deep copy, except the style is shallow-copied.
     */
    public TextComponentKeybind createCopy() {
        TextComponentKeybind textcomponentkeybind = new TextComponentKeybind(this.keybind);
        textcomponentkeybind.setStyle(this.getStyle().createShallowCopy());

        for (ITextComponent itextcomponent : this.getSiblings()) {
            textcomponentkeybind.appendSibling(itextcomponent.createCopy());
        }

        return textcomponentkeybind;
    }

    public boolean equals(Object p_equals_1_) {
        if (this == p_equals_1_) {
            return true;
        } else if (!(p_equals_1_ instanceof TextComponentKeybind)) {
            return false;
        } else {
            TextComponentKeybind textcomponentkeybind = (TextComponentKeybind) p_equals_1_;
            return this.keybind.equals(textcomponentkeybind.keybind) && super.equals(p_equals_1_);
        }
    }

    public String toString() {
        return "KeybindComponent{keybind='" + this.keybind + '\'' + ", siblings=" + this.siblings + ", style=" + this.getStyle() + '}';
    }

    public String getKeybind() {
        return this.keybind;
    }
}