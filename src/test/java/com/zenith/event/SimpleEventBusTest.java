package com.zenith.event;

import com.zenith.util.Wait;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.zenith.util.Pair.of;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimpleEventBusTest {

    @Test
    public void unsubscribeTest() {
        final SimpleEventBus bus = new SimpleEventBus();
        final Foo foo = new Foo();
        final Bar bar = new Bar();
        Subscription fooSubscription = foo.subscribe(bus);
        Subscription barSubscription = bar.subscribe(bus);
        bus.post(new TestEvent());

        assertEquals(1, foo.counter.get());
        assertEquals(1, bar.counter.get());

        fooSubscription.unsubscribe();
        bus.post(new TestEvent());

        assertEquals(1, foo.counter.get());
        assertEquals(2, bar.counter.get());
    }

    @Test
    public void subscribeMultipleEventsTest() {
        final SimpleEventBus bus = new SimpleEventBus();
        final Baz baz = new Baz();
        final Bar bar = new Bar();
        Subscription barSubscription = bar.subscribe(bus);
        Subscription bazSubscription = baz.subscribe(bus);
        bus.post(new TestEvent());
        bus.post(new AnotherTestEvent());
        assertEquals(2, baz.counter.get());
        assertEquals(1, bar.counter.get());
        bazSubscription.unsubscribe();
        bus.post(new TestEvent());
        bus.post(new AnotherTestEvent());
        assertEquals(2, baz.counter.get());
        assertEquals(2, bar.counter.get());
    }

    @Test
    public void postAsyncTest() {
        final SimpleEventBus bus = new SimpleEventBus();
        final Foo foo = new Foo();
        Subscription fooSubscription = foo.subscribe(bus);
        bus.postAsync(new TestEvent());
        Wait.waitUntilCondition(() -> foo.counter.get() == 1, 1000);
        assertEquals(1, foo.counter.get());
        fooSubscription.unsubscribe();
        bus.postAsync(new TestEvent());
        Wait.waitALittle(1);
        assertEquals(1, foo.counter.get());
    }

    public record TestEvent() { }
    public record AnotherTestEvent() { }

    public static class Foo {
        AtomicInteger counter = new AtomicInteger(0);

        public Subscription subscribe(SimpleEventBus bus) {
            return bus.subscribe(TestEvent.class, this::handleTestEvent);
        }

        public void handleTestEvent(final TestEvent testEvent) {
            counter.incrementAndGet();
        }
    }

    public static class Bar {
        AtomicInteger counter = new AtomicInteger(0);

        public Subscription subscribe(SimpleEventBus bus) {
            return bus.subscribe(TestEvent.class, this::handleTestEvent);
        }

        public void handleTestEvent(final TestEvent testEvent) {
            counter.incrementAndGet();
        }
    }

    public static class Baz {
        AtomicInteger counter = new AtomicInteger(0);

        public Subscription subscribe(SimpleEventBus bus) {
            return bus.subscribe(
                of(TestEvent.class, (Consumer<TestEvent>) this::handleTestEvent),
                of(AnotherTestEvent.class, (Consumer<AnotherTestEvent>) this::handleAnotherTestEvent));
        }

        public void handleTestEvent(final TestEvent testEvent) {
            counter.incrementAndGet();
        }

        public void handleAnotherTestEvent(final AnotherTestEvent event) {
            counter.incrementAndGet();
        }
    }
}
