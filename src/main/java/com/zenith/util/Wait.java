package com.zenith.util;

import lombok.SneakyThrows;

import java.time.Instant;
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
        final var beforeTime = Instant.now().getEpochSecond();
        while (!conditionSupplier.get() && Instant.now().getEpochSecond() - beforeTime < secondsToWait) {
            Wait.waitMs(50);
        }
        return conditionSupplier.get();
    }

    public static void waitRandomMs(final int ms) {
        Wait.waitMs((int) (Math.random() * ms));
    }
}
