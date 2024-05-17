package com.zenith.util;

import lombok.Data;
import lombok.NonNull;

import java.lang.reflect.Array;

/**
 * An ArrayList implementation optimized for fast, direct, array access.
 *
 * Modifications have greatly increased overhead, implemented as copy-on-write
 *
 * For best performance, this list should be rarely mutated
 *
 * This class implements a very limited subset of the List interface, it's not intended to be a drop-in replacement for ArrayList or List
 */
@Data
public class FastArrayList<T> {
    // Access this array through the generated getter for iterations and read operations. Write ops only through the add/remove methods
    protected T @NonNull [] array;
    // can't allocate typed array without class reference
    // Java's ArrayList always allocates Object[] and casts elements returned by getters
    // but we're avoiding that here as we want consumers to access the underlying array reference directly
    protected final Class<T> clazz;

    public FastArrayList(@NonNull Class<T> clazz) {
        this.clazz = clazz;
        this.array = (T[]) Array.newInstance(clazz, 0);
    }

    public int size() {
        return array.length;
    }

    public boolean isEmpty() {
        return array.length == 0;
    }

    public boolean contains(final Object o) {
        var a = array;
        for (int i = 0; i < a.length; i++) {
            if (a[i].equals(o)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void add(@NonNull T element) {
        T[] newArray = (T[]) Array.newInstance(clazz, this.array.length + 1);
        System.arraycopy(this.array, 0, newArray, 0, this.array.length);
        newArray[this.array.length] = element;
        this.array = newArray;
    }

    public synchronized boolean remove(@NonNull T element) {
        var a = array;
        int index = -1;
        for (int i = 0; i < a.length; i++) {
            if (a[i].equals(element)) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            return false;
        }
        T[] newArray = (T[]) Array.newInstance(clazz, a.length - 1);
        System.arraycopy(a, 0, newArray, 0, index);
        System.arraycopy(a, index + 1, newArray, index, a.length - index - 1);
        this.array = newArray;
        return true;
    }

    public void clear() {
        this.array = (T[]) Array.newInstance(clazz, 0);
    }
}
