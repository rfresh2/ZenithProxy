package com.zenith.feature.deathmessages;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class WordIterator implements Iterator<String> {

    private final List<String> words;
    private int index;

    public WordIterator(final List<String> input) {
        this.index = 0;
        this.words = input;
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

    public String peek2() {
        try {
            return words.get(index + 1);
        } catch (final IndexOutOfBoundsException e) {
            return null;
        }
    }

    public List<String> peekToEnd() {
        try {
            return words.subList(index, words.size());
        } catch (final IndexOutOfBoundsException e) {
            return Collections.emptyList();
        }
    }
}
