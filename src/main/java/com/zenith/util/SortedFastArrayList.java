package com.zenith.util;

import lombok.NonNull;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Comparator;

public class SortedFastArrayList<T> extends FastArrayList<T> {
    private final Comparator<T> comparator;
    public SortedFastArrayList(@NonNull final Class<T> clazz, Comparator<T> comparator) {
        super(clazz);
        this.comparator = comparator;
    }

    @Override
    public void add(@NonNull T element) {
        T[] newArray = (T[]) Array.newInstance(clazz, this.array.length + 1);
        System.arraycopy(this.array, 0, newArray, 0, this.array.length);
        newArray[this.array.length] = element;
        Arrays.sort(newArray, comparator);
        this.array = newArray;
    }
}
