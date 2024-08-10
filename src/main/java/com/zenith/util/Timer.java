package com.zenith.util;

public class Timer {

    public static Timer createTickTimer() {
        return new Timer(50L);
    }

    private long time = System.currentTimeMillis();
    private final long tickTimeMs;

    public Timer() {
        this.tickTimeMs = 1L;
    }

    public Timer(final long tickTimeMs) {
        this.tickTimeMs = tickTimeMs;
    }

    public void reset() {
        this.time = System.currentTimeMillis();
    }

    public boolean tick(final long delay) {
        return tick(delay, true);
    }

    public boolean tick(final long delay, final boolean resetIfTick) {
        if (System.currentTimeMillis() - this.time > delay * tickTimeMs) {
            if (resetIfTick) this.time = System.currentTimeMillis();
            return true;
        } else {
            return false;
        }
    }
}
