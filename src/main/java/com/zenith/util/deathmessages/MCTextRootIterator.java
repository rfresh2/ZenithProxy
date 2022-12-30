package com.zenith.util.deathmessages;

import net.daporkchop.lib.logging.format.component.TextComponent;
import net.daporkchop.lib.minecraft.text.component.MCTextRoot;

import java.awt.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static java.util.Objects.isNull;

public final class MCTextRootIterator implements Iterator<MCTextWord> {
    final MCTextRoot mcTextRoot;
    private int childIndex = 0;
    private int wordIndex = 0;

    public MCTextRootIterator(final MCTextRoot mcTextRoot) {
        this.mcTextRoot = mcTextRoot;
    }

    @Override
    public boolean hasNext() {
        // todo: this is kinda hacky. don't actually use this.
        //  this will do the entire computation for next()
        final int beforeChildIndex = childIndex;
        final int beforeWordIndex = wordIndex;
        final MCTextWord next = next();
        childIndex = beforeChildIndex;
        wordIndex = beforeWordIndex;
        if (isNull(next)) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public MCTextWord next() {
        if (mcTextRoot.getChildren().size() > childIndex) {
            final TextComponent childComponent = mcTextRoot.getChildren().get(childIndex);
            final List<String> words = Arrays.asList(childComponent.toRawString().split(" "));
            if (words.size() > wordIndex) {
                boolean isKeyword = !childComponent.getColor().equals(new Color(170, 0, 0));
                String word = words.get(wordIndex);
                if (isKeyword) {
                    if (word.startsWith("'s")) { // special case where player names have possession modifier in a child
                        wordIndex++;
                        return next();
                    }
                    // weapons can have multiple words
                    word = String.join(" ", words.toArray(new String[0]));
                    wordIndex = Integer.MAX_VALUE;
                } else {
                    wordIndex++;
                    if (word.isEmpty())
                        return next(); // if child words starts with " " then split will have an empty string here
                }
                return new MCTextWord(isKeyword, word);
            } else {
                childIndex++;
                wordIndex = 0;
                return next();
            }
        } else {
            return null;
        }
    }
}
