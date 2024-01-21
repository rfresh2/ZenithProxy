package com.zenith.event;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import java.util.Arrays;
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
 *
 * Event priority is ordered by larger priority ints being called first.
 * The default priority is 0.
 */
public class SimpleEventBus {

    private final ExecutorService asyncEventExecutor;
    private final Reference2ObjectMap<Class<?>, EventConsumer<?>[]> eventConsumersMap = new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectMap<Object, Subscription> subscribersMap = new Reference2ObjectOpenHashMap<>();

    public SimpleEventBus(final ExecutorService asyncEventExecutor) {
        this.asyncEventExecutor = asyncEventExecutor;
    }

    public <T> void subscribe(Object subscriber, Class<T> eventType, Consumer<T> handler) {
        var existingSub = subscribersMap.remove(subscriber);
        if (existingSub != subscribersMap.defaultReturnValue()) existingSub.unsubscribe();
        var sub = subscribe(EventConsumer.of(eventType, handler));
        subscribersMap.put(subscriber, sub);
    }

    public <T> void subscribe(Object subscriber, EventConsumer<T> eventConsumer) {
        var existingSub = subscribersMap.remove(subscriber);
        if (existingSub != subscribersMap.defaultReturnValue()) existingSub.unsubscribe();
        var sub = subscribe(eventConsumer);
        subscribersMap.put(subscriber, sub);
    }

    public final void subscribe(Object subscriber, EventConsumer<?>... eventConsumers) {
        var existingSub = subscribersMap.remove(subscriber);
        if (existingSub != subscribersMap.defaultReturnValue()) existingSub.unsubscribe();
        var sub = subscribe(eventConsumers);
        subscribersMap.put(subscriber, sub);
    }

    public boolean isSubscribed(Object subscriber) {
        return subscribersMap.containsKey(subscriber);
    }

    public void unsubscribe(Object subscriber) {
        var sub = subscribersMap.remove(subscriber);
        if (sub != subscribersMap.defaultReturnValue()) sub.unsubscribe();
    }

    // handlers can throw and return exceptions - cancelling subsequent event executions
    public <T> void post(T event) {
        var consumers = eventConsumersMap.get(event.getClass());
        if (consumers != eventConsumersMap.defaultReturnValue()) {
            for (int i = 0; i < consumers.length; i++) {
                var consumer = consumers[i];
                ((Consumer<T>) consumer.handler()).accept(event);
            }
        }
    }

    public <T> void postAsync(T event) {
        var consumers = eventConsumersMap.get(event.getClass());
        if (consumers != eventConsumersMap.defaultReturnValue())
            asyncEventExecutor.execute(() -> this.postAsyncInternal(event, consumers));
    }

    private synchronized void removeEventConsumer(EventConsumer<?> eventConsumer) {
        var consumers = eventConsumersMap.get(eventConsumer.eventClass());
        if (consumers != eventConsumersMap.defaultReturnValue()) {
            int index = -1;
            for (int i = 0; i < consumers.length; i++) {
                if (consumers[i].handler() == eventConsumer.handler()) {
                    index = i;
                    break;
                }
            }
            if (index == -1) return;
            if (consumers.length == 1) {
                eventConsumersMap.remove(eventConsumer.eventClass());
                return;
            }
            final EventConsumer<?>[] newConsumers = new EventConsumer[consumers.length - 1];
            System.arraycopy(consumers, 0, newConsumers, 0, index);
            System.arraycopy(consumers, index + 1, newConsumers, index, consumers.length - index - 1);
            eventConsumersMap.put(eventConsumer.eventClass(), newConsumers);
        }
    }

    @SafeVarargs
    private synchronized Subscription subscribe(EventConsumer<?>... eventConsumers) {
        for (int i = 0; i < eventConsumers.length; i++) {
            var eventConsumer = eventConsumers[i];
            this.eventConsumersMap.compute(eventConsumer.eventClass(), (key, consumers) -> {
                if (consumers == null) {
                    return new EventConsumer[]{eventConsumer};
                } else {
                    final EventConsumer<?>[] newConsumers = new EventConsumer[consumers.length + 1];
                    System.arraycopy(consumers, 0, newConsumers, 0, consumers.length);
                    newConsumers[consumers.length] = eventConsumer;
                    Arrays.sort(newConsumers);
                    return newConsumers;
                }
            });
        }
        return new Subscription(() -> {
            for (int i = 0; i < eventConsumers.length; i++) {
                removeEventConsumer(eventConsumers[i]);
            }
        });
    }

    private synchronized <T> Subscription subscribe(EventConsumer<T> eventConsumer) {
        eventConsumersMap.compute(eventConsumer.eventClass(), (key, consumers) -> {
            if (consumers == null) {
                return new EventConsumer[]{eventConsumer};
            } else {
                final EventConsumer<?>[] newConsumers = new EventConsumer[consumers.length + 1];
                System.arraycopy(consumers, 0, newConsumers, 0, consumers.length);
                newConsumers[consumers.length] = eventConsumer;
                Arrays.sort(newConsumers);
                return newConsumers;
            }
        });
        return new Subscription(() -> removeEventConsumer(eventConsumer));
    }

    private <T> void postAsyncInternal(T event, EventConsumer<?>[] eventConsumers) {
        try {
            for (int i = 0; i < eventConsumers.length; i++) {
                var consumer = eventConsumers[i];
                ((Consumer<T>) consumer.handler()).accept(event);
            }
        } catch (final Throwable e) { // swallow exception so we don't kill the executor
            DEFAULT_LOG.debug("Error handling async event", e);
        }
    }
}
