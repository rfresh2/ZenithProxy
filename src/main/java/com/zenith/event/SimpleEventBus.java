package com.zenith.event;

import com.zenith.util.Pair;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import static com.zenith.Shared.DEFAULT_LOG;

/**
 * A simple event bus without reflection.
 *
 * Subscriptions are owned by object references.
 * It's important to unsubscribe when the object is no longer needed.
 * Failing to do this will block GC of the object and existing event handlers will still be called
 *
 * There is no thread safety built-in to event (un)subscriptions methods, avoid subscribing the same object from multiple threads
 */
public class SimpleEventBus {

    private final ExecutorService asyncEventExecutor;
    private final Reference2ObjectMap<Class<?>, Consumer<?>[]> handlers = new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectMap<Object, Subscription> subscribers = new Reference2ObjectOpenHashMap<>();

    public SimpleEventBus(final ExecutorService asyncEventExecutor) {
        this.asyncEventExecutor = asyncEventExecutor;
    }

    public <T> void subscribe(Object subscriber, Class<T> eventType, Consumer<T> handler) {
        var existingSub = subscribers.remove(subscriber);
        if (existingSub != subscribers.defaultReturnValue()) existingSub.unsubscribe();
        var sub = subscribe(eventType, handler);
        subscribers.put(subscriber, sub);
    }

    @SafeVarargs
    public final void subscribe(Object subscriber, Pair<Class<?>, Consumer<?>>... pairs) {
        var existingSub = subscribers.remove(subscriber);
        if (existingSub != subscribers.defaultReturnValue()) existingSub.unsubscribe();
        var sub = subscribe(pairs);
        subscribers.put(subscriber, sub);
    }

    public boolean isSubscribed(Object subscriber) {
        return subscribers.containsKey(subscriber);
    }

    public static <T> Pair<Class<?>, Consumer<?>> pair(Class<T> clazz, Consumer<T> handler) {
        return Pair.of(clazz, handler);
    }

    public void unsubscribe(Object subscriber) {
        var sub = subscribers.remove(subscriber);
        if (sub != subscribers.defaultReturnValue()) sub.unsubscribe();
    }

    // handlers can throw and return exceptions - cancelling subsequent event executions
    public <T> void post(T event) {
        var consumers = handlers.get(event.getClass());
        if (consumers != handlers.defaultReturnValue()) {
            for (int i = 0; i < consumers.length; i++) {
                var consumer = consumers[i];
                ((Consumer<T>) consumer).accept(event);
            }
        }
    }

    public <T> void postAsync(T event) {
        var consumers = handlers.get(event.getClass());
        if (consumers != handlers.defaultReturnValue())
            asyncEventExecutor.execute(() -> this.postAsyncInternal(event, consumers));
    }

    private synchronized void removeHandler(Class<?> eventType, Consumer<?> handler) {
        var consumers = handlers.get(eventType);
        if (consumers != handlers.defaultReturnValue()) {
            int index = -1;
            for (int i = 0; i < consumers.length; i++) {
                if (consumers[i] == handler) {
                    index = i;
                    break;
                }
            }
            if (index == -1) return;
            if (consumers.length == 1) {
                handlers.remove(eventType);
                return;
            }
            final Consumer<?>[] newConsumers = new Consumer[consumers.length - 1];
            System.arraycopy(consumers, 0, newConsumers, 0, index);
            System.arraycopy(consumers, index + 1, newConsumers, index, consumers.length - index - 1);
            handlers.put(eventType, newConsumers);
        }
    }

    private synchronized <T> Subscription subscribe(Class<T> eventType, Consumer<T> handler) {
        handlers.compute(eventType, (key, consumers) -> {
            if (consumers == null) {
                return new Consumer<?>[]{handler};
            } else {
                final Consumer<?>[] newConsumers = new Consumer[consumers.length + 1];
                System.arraycopy(consumers, 0, newConsumers, 0, consumers.length);
                newConsumers[consumers.length] = handler;
                return newConsumers;
            }
        });
        return new Subscription(() -> removeHandler(eventType, handler));
    }

    @SafeVarargs
    private synchronized Subscription subscribe(Pair<Class<?>, Consumer<?>>... pairs) {
        for (int i = 0; i < pairs.length; i++) {
            var pair = pairs[i];
            handlers.compute(pair.left(), (key, consumers) -> {
                if (consumers == null) {
                    return new Consumer<?>[]{pair.right()};
                } else {
                    final Consumer<?>[] newConsumers = new Consumer[consumers.length + 1];
                    System.arraycopy(consumers, 0, newConsumers, 0, consumers.length);
                    newConsumers[consumers.length] = pair.right();
                    return newConsumers;
                }
            });
        }
        return new Subscription(() -> {
            for (int i = 0; i < pairs.length; i++) {
                var pair = pairs[i];
                removeHandler(pair.left(), pair.right());
            }
        });
    }

    private <T> void postAsyncInternal(T event, Consumer<?>[] consumers) {
        try {
            for (int i = 0; i < consumers.length; i++) {
                var consumer = consumers[i];
                ((Consumer<T>) consumer).accept(event);
            }
        } catch (final Throwable e) { // swallow exception so we don't kill the executor
            DEFAULT_LOG.debug("Error handling async event", e);
        }
    }
}
