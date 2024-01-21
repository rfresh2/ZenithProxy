package com.zenith.event;

import java.util.function.Consumer;

public record EventConsumer<T>(Class<T> eventClass, int priority, Consumer<T> handler) implements Comparable<EventConsumer<T>> {
    @Override
    public int compareTo(EventConsumer o) {
        return Integer.compare(priority, o.priority);
    }

    public static <T> EventConsumer<T> of(Class<T> clazz, Consumer<T> handler) {
        return new EventConsumer<>(clazz, 0, handler);
    }

    public static <T> EventConsumer<T> of(Class<T> clazz, int priority, Consumer<T> handler) {
        return new EventConsumer<>(clazz, priority, handler);
    }
}
