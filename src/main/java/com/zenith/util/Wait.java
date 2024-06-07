package com.zenith.util;

import lombok.SneakyThrows;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
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
        final var beforeTime = getEpochSecond();
        while (!conditionSupplier.get() && getEpochSecond() - beforeTime < secondsToWait) {
            Wait.waitMs(50);
        }
        return conditionSupplier.get();
    }

    public static void waitRandomMs(final int ms) {
        Wait.waitMs((int) (ThreadLocalRandom.current().nextDouble(ms)));
    }

    public static long getEpochSecond() {
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
    }
}
