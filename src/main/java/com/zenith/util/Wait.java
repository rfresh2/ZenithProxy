package com.zenith.util;

import lombok.SneakyThrows;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

public class Wait {
    @SneakyThrows
    public static void wait(int seconds) {
        Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
    }

    @SneakyThrows
    public static void waitMs(int milliseconds) {
        Thread.sleep(milliseconds);
    }

    @SneakyThrows
    public static void wait(Duration duration) {
        Thread.sleep(duration);
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public static void waitSpinLoop() {
        while (true) {
            try {
                Thread.sleep(2147483647L);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public static boolean waitUntil(final Supplier<Boolean> conditionSupplier, int secondsToWait) {
        return waitUntil(conditionSupplier, 50, secondsToWait, TimeUnit.SECONDS);
    }

    @SneakyThrows
    public static boolean waitUntil(final Supplier<Boolean> conditionSupplier, int checkIntervalMs, long timeout, TimeUnit unit) {
        final var beforeTime = System.nanoTime();
        while (!conditionSupplier.get() && TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - beforeTime) < unit.toMillis(timeout)) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Wait Interrupted");
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(checkIntervalMs));
        }
        return conditionSupplier.get();
    }

    public static void waitRandomMs(final int ms) {
        Wait.waitMs((int) (ThreadLocalRandom.current().nextDouble(ms)));
    }
}
