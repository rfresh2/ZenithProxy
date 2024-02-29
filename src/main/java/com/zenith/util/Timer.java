package com.zenith.util;

import java.time.Instant;

public class Timer {

    Instant time = Instant.now();

    public Timer() {}

    public void reset() {
        this.time = Instant.now();
    }

    public long tickTimeConstant() {
        return 1L;
    }

    public boolean tick(final long delay, final boolean resetIfTick) {
        if (Instant.now().toEpochMilli() - this.time.toEpochMilli() > delay * tickTimeConstant()) {
            if (resetIfTick) {
                this.time = Instant.now();
            }
            return true;
        } else {
            return false;
        }
    }
}
