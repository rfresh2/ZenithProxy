package com.zenith.util;

import java.time.Instant;

public class Timer {

    Instant time = Instant.now();

    public Timer() {}

    public void reset() {
        this.time = Instant.now();
    }
}
