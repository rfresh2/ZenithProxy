package com.zenith.event;

import com.zenith.util.Pair;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

/**
 * A simple event bus without reflection.
 *
 * We need to avoid reflection where possible due to GraalVM native compilation.
 * Otherwise, anytime we add event handlers we would need to update the compilation configs and whatnot.
 * which would just create a bunch of footguns for developers.
 *
 * The main drawback to this approach is we need to manage the lifecycle of the subscriptions ourselves.
 * Any object that has methods subscribed MUST unsubscribe itself before the object is garbage collected.
 * In other words, all objects with subscriptions must have references retained somewhere or be unsubscribed before removing references.
 */
public class SimpleEventBus {

    private final ConcurrentHashMap<Class<?>, List<Consumer<?>>> handlers = new ConcurrentHashMap<>();

    public <T> Subscription subscribe(Class<T> eventType, Consumer<T> handler) {
        handlers.computeIfAbsent(eventType, key -> new CopyOnWriteArrayList<>()).add(handler);
        return new Subscription(() -> unsubscribe(eventType, handler));
    }

    @SafeVarargs
    public final Subscription subscribe(Pair<Class<?>, Consumer<?>>... pairs) {
        for (Pair<Class<?>, Consumer<?>> pair : pairs) {
            handlers.computeIfAbsent(pair.left(), key -> new CopyOnWriteArrayList<>()).add(pair.right());
        }
        return new Subscription(() -> {
            for (Pair<Class<?>, Consumer<?>> pair : pairs) {
                unsubscribe(pair.left(), pair.right());
            }
        });
    }

    public static <T> Pair<Class<?>, Consumer<?>> pair(Class<T> clazz, Consumer<T> handler) {
        return Pair.of(clazz, handler);
    }

    public void unsubscribe(Class<?> eventType, Consumer<?> handler) {
        List<Consumer<?>> consumers = handlers.get(eventType);
        if (consumers != null) {
            consumers.remove(handler);
            if (consumers.isEmpty()) {
                handlers.remove(eventType);
            }
        }
    }

    public <T> void post(T event) {
        List<Consumer<?>> consumers = handlers.get(event.getClass());
        if (consumers != null) {
            for (Consumer<?> consumer : consumers) {
                ((Consumer<T>) consumer).accept(event);
            }
        }
    }

    public <T> void postAsync(T event) {
        List<Consumer<?>> consumers = handlers.get(event.getClass());
        if (consumers != null) {
            ForkJoinPool.commonPool().execute(() -> {
                for (Consumer<?> consumer : consumers) {
                    ((Consumer<T>) consumer).accept(event);
                }
            });
        }
    }
}
