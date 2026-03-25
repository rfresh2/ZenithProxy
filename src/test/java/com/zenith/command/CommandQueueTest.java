package com.zenith.command;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class CommandQueueTest {

    @Test
    public void executesCommandsSequentially() throws Exception {
        try (var queue = new CommandQueue("command-queue-test-%d", 4)) {
            var order = Collections.synchronizedList(new ArrayList<String>());
            var firstStarted = new CountDownLatch(1);
            var releaseFirst = new CountDownLatch(1);
            var secondFinished = new CountDownLatch(1);
            var failure = new AtomicReference<Throwable>();

            var firstSubmission = queue.submit(() -> {
                order.add("first-start");
                firstStarted.countDown();
                try {
                    if (!releaseFirst.await(2, TimeUnit.SECONDS)) {
                        failure.compareAndSet(null, new AssertionError("Timed out waiting to release first command"));
                        return;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failure.compareAndSet(null, e);
                    return;
                }
                order.add("first-end");
            });
            assertTrue(firstSubmission.accepted());
            assertEquals(0, firstSubmission.commandsAhead());
            assertTrue(firstStarted.await(2, TimeUnit.SECONDS));

            var secondSubmission = queue.submit(() -> {
                order.add("second");
                secondFinished.countDown();
            });
            assertTrue(secondSubmission.accepted());
            assertEquals(1, secondSubmission.commandsAhead());
            assertFalse(secondFinished.await(200, TimeUnit.MILLISECONDS));

            releaseFirst.countDown();
            assertTrue(secondFinished.await(2, TimeUnit.SECONDS));
            assertNull(failure.get());
            assertEquals(List.of("first-start", "first-end", "second"), order);
        }
    }

    @Test
    public void rejectsCommandsWhenPendingQueueIsFull() throws Exception {
        try (var queue = new CommandQueue("command-queue-test-%d", 1)) {
            var releaseFirst = new CountDownLatch(1);
            var firstStarted = new CountDownLatch(1);
            var failure = new AtomicReference<Throwable>();

            var firstSubmission = queue.submit(() -> {
                firstStarted.countDown();
                try {
                    if (!releaseFirst.await(2, TimeUnit.SECONDS)) {
                        failure.compareAndSet(null, new AssertionError("Timed out waiting to release first command"));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failure.compareAndSet(null, e);
                }
            });
            assertTrue(firstSubmission.accepted());
            assertTrue(firstStarted.await(2, TimeUnit.SECONDS));

            var secondSubmission = queue.submit(() -> {});
            assertTrue(secondSubmission.accepted());
            assertEquals(1, secondSubmission.commandsAhead());

            var thirdSubmission = queue.submit(() -> {});
            assertFalse(thirdSubmission.accepted());
            assertEquals(2, thirdSubmission.commandsAhead());

            releaseFirst.countDown();
            Thread.sleep(100);
            assertNull(failure.get());
        }
    }
}
