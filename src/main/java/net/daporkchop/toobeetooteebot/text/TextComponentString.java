package net.daporkchop.toobeetooteebot.text;

public class TextComponentString extends TextComponentBase {
    private final String text;

    public TextComponentString(String msg) {
        this.text = msg;
    }

    /**
     * Gets the text value of this component. This is used to access the {@link #text} property, and only should be used
     * when dealing specifically with instances of {@link TextComponentString} - for other purposes, use {@link
     * #getUnformattedComponentText()}.
     */
    public String getText() {
        return this.text;
    }

    /**
     * Gets the raw content of this component (but not its sibling components), without any formatting codes. For
     * example, this is the raw text in a {@link TextComponentString}, but it's the translated text for a {@link
     * TextComponentTranslation} and it's the score value for a {@link TextComponentScore}.
     */
    public String getUnformattedComponentText() {
        return this.text;
    }

    /**
     * Creates a copy of this component.  Almost a deep copy, except the style is shallow-copied.
     */
    public TextComponentString createCopy() {
        TextComponentString textcomponentstring = new TextComponentString(this.text);
        textcomponentstring.setStyle(this.getStyle().createShallowCopy());

        for (ITextComponent itextcomponent : this.getSiblings()) {
            textcomponentstring.appendSibling(itextcomponent.createCopy());
        }

        return textcomponentstring;
    }

    public boolean equals(Object p_equals_1_) {
        if (this == p_equals_1_) {
            return true;
        } else if (!(p_equals_1_ instanceof TextComponentString)) {
            return false;
        } else {
            TextComponentString textcomponentstring = (TextComponentString) p_equals_1_;
            return this.text.equals(textcomponentstring.getText()) && super.equals(p_equals_1_);
        }
    }

    public String toString() {
        return "TextComponent{text='" + this.text + '\'' + ", siblings=" + this.siblings + ", style=" + this.getStyle() + '}';
    }
}