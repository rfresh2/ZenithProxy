package com.zenith.util.deathmessages;

import java.util.Iterator;
import java.util.List;

import static com.zenith.util.deathmessages.DeathMessageSchemaInstance.spaceSplit;

public class WordIterator implements Iterator<String> {

    private final List<String> words;
    private int index;

    public WordIterator(final String input) {
        this.index = 0;
        this.words = spaceSplit(input);
    }

    @Override
    public boolean hasNext() {
        return index < words.size();
    }

    @Override
    public String next() {
        try {
            return words.get(index++);
        } catch (final IndexOutOfBoundsException e) {
            return null;
        }
    }

    public String peek() {
        try {
            return words.get(index);
        } catch (final IndexOutOfBoundsException e) {
            return null;
        }
    }
}
