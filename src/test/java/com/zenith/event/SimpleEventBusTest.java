package com.zenith.event;

import com.zenith.util.Wait;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static com.zenith.event.SimpleEventBus.pair;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimpleEventBusTest {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Test
    public void unsubscribeTest() {
        final SimpleEventBus bus = new SimpleEventBus(executorService);
        final Foo foo = new Foo();
        final Bar bar = new Bar();
        foo.subscribe(bus);
        bar.subscribe(bus);
        bus.post(new TestEvent());

        assertEquals(1, foo.counter.get());
        assertEquals(1, bar.counter.get());

        bus.unsubscribe(foo);
        bus.post(new TestEvent());

        assertEquals(1, foo.counter.get());
        assertEquals(2, bar.counter.get());

        bus.unsubscribe(bar);
        bus.post(new TestEvent());

        assertEquals(1, foo.counter.get());
        assertEquals(2, bar.counter.get());
    }

    @Test
    public void subscribeMultipleEventsTest() {
        final SimpleEventBus bus = new SimpleEventBus(executorService);
        final Baz baz = new Baz();
        final Bar bar = new Bar();
        bar.subscribe(bus);
        baz.subscribe(bus);
        bus.post(new TestEvent());
        bus.post(new AnotherTestEvent());
        assertEquals(2, baz.counter.get());
        assertEquals(1, bar.counter.get());
        bus.unsubscribe(baz);
        bus.post(new TestEvent());
        bus.post(new AnotherTestEvent());
        assertEquals(2, baz.counter.get());
        assertEquals(2, bar.counter.get());
    }

    @Test
    public void postAsyncTest() {
        final SimpleEventBus bus = new SimpleEventBus(executorService);
        final Foo foo = new Foo();
        foo.subscribe(bus);
        bus.postAsync(new TestEvent());
        Wait.waitUntilCondition(() -> foo.counter.get() == 1, 1000);
        assertEquals(1, foo.counter.get());
        bus.unsubscribe(foo);
        bus.postAsync(new TestEvent());
        Wait.waitALittle(1);
        assertEquals(1, foo.counter.get());
    }

    public record TestEvent() { }
    public record AnotherTestEvent() { }

    public static class Foo {
        AtomicInteger counter = new AtomicInteger(0);

        public void subscribe(SimpleEventBus bus) {
            bus.subscribe(this, TestEvent.class, this::handleTestEvent);
        }

        public void handleTestEvent(final TestEvent testEvent) {
            counter.incrementAndGet();
        }
    }

    public static class Bar {
        AtomicInteger counter = new AtomicInteger(0);

        public void subscribe(SimpleEventBus bus) {
            bus.subscribe(this, TestEvent.class, this::handleTestEvent);
        }

        public void handleTestEvent(final TestEvent testEvent) {
            counter.incrementAndGet();
        }
    }

    public static class Baz {
        AtomicInteger counter = new AtomicInteger(0);

        public void subscribe(SimpleEventBus bus) {
            bus.subscribe(this,
                pair(TestEvent.class, this::handleTestEvent),
                pair(AnotherTestEvent.class, this::handleAnotherTestEvent));
        }

        public void handleTestEvent(final TestEvent testEvent) {
            counter.incrementAndGet();
        }

        public void handleAnotherTestEvent(final AnotherTestEvent event) {
            counter.incrementAndGet();
        }
    }
}
