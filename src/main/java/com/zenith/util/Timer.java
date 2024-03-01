package com.zenith.util;

import java.time.Instant;

public class Timer {

    public static Timer newTickTimer() {
        return new Timer(50L);
    }

    private Instant time = Instant.now();
    private final long tickTimeMs;

    public Timer() {
        this.tickTimeMs = 1L;
    }

    public Timer(final long tickTimeMs) {
        this.tickTimeMs = tickTimeMs;
    }

    public void reset() {
        this.time = Instant.now();
    }

    public boolean tick(final long delay) {
        return tick(delay, true);
    }

    public boolean tick(final long delay, final boolean resetIfTick) {
        if (Instant.now().toEpochMilli() - this.time.toEpochMilli() > delay * tickTimeMs) {
            if (resetIfTick) this.time = Instant.now();
            return true;
        } else {
            return false;
        }
    }
}
