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