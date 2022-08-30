package com.zenith.util;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class Wait {
    public static void waitALittle(int seconds) {
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void waitALittleMs(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void waitSpinLoop() {
        while (true) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public static boolean waitUntilCondition(final Supplier<Boolean> conditionSupplier, int secondsToWait) {
        long beforeTime = Instant.now().getEpochSecond();
        while (!conditionSupplier.get() && Instant.now().getEpochSecond() - beforeTime < secondsToWait) {
            Wait.waitALittleMs(50);
        }
        return conditionSupplier.get();
    }

    private Wait() {}
}
