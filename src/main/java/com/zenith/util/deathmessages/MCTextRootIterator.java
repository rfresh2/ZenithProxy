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
                if (word.isEmpty()) {
                    wordIndex++;
                    return next(); // if child words starts with " " then split will have an empty string here
                }
                if (isKeyword) {
                    if (word.startsWith("'s")) { // special case where player names have possession modifier in a child
                        wordIndex++;
                        return next();
                    }
                    // weapons can have multiple words
                    word = String.join(" ", words.toArray(new String[0]));
                    wordIndex = Integer.MAX_VALUE;
                } else {
                    String finalWord = word;
                    if (DeathMessageSchemaInstance.mobTypes.stream().anyMatch(mt -> Arrays.asList(mt.split(" ")).stream().anyMatch(mts -> mts.equals(finalWord.replace(".", ""))))) {
                        // greedily look for a second word
                        // todo: i don't like this. if there's a mob type word that isn't unique, things will go very wrong
                        if (words.size() > wordIndex + 1) {
                            // this doesn't prevent matching on different mob types
                            // todo: we want to preserve mob type word index on matching
                            //  would also be nice to percolate the mob type up so we can set it as the killer
                            if (DeathMessageSchemaInstance.mobTypes.stream().anyMatch(mt -> Arrays.asList(mt.split(" ")).stream().anyMatch(mts -> mts.equals(words.get(wordIndex + 1).replace(".", ""))))) {
                                wordIndex += 2; // skip ahead
                                return new MCTextWord(isKeyword, word + " " + words.get(wordIndex - 2 + 1));
                            }
                        }
                    }
                    wordIndex++;
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
