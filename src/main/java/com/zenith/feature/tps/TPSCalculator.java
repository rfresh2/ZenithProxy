package com.zenith.feature.tps;

import com.google.common.primitives.Doubles;
import com.zenith.event.proxy.ConnectEvent;

import java.util.Arrays;

import static com.zenith.Shared.EVENT_BUS;

public class TPSCalculator {

    private static final int TICK_RATES_SIZE = 60; // 1 sample per second - so 1 minute
    // circularly written array
    private final double[] tickRates = new double[TICK_RATES_SIZE];
    private int nextTickIndex = 0;
    private long timeSinceLastTimeUpdate = -1L;

    public TPSCalculator() {
        reset();
        // calculators must always be reused
        // beware: event sub is never unsubbed
        EVENT_BUS.subscribe(this, ConnectEvent.class, (e) -> reset());
    }

    // expected to be received once per second by the mc server
    public void handleTimeUpdate() {
        if (timeSinceLastTimeUpdate != -1L) {
            final double timeElapsed = (System.nanoTime() - timeSinceLastTimeUpdate) / 1E9;
            final double tps = Doubles.constrainToRange(20.0 / timeElapsed, 0.0, 20.0);
            synchronized (tickRates) {
                tickRates[nextTickIndex] = tps;
                if (++nextTickIndex >= tickRates.length) nextTickIndex = 0;
            }
        }
        timeSinceLastTimeUpdate = System.nanoTime();
    }

    public void reset() {
        synchronized (tickRates) {
            // fill with 20.0 tps by default
            Arrays.fill(tickRates, 20.0);
            nextTickIndex = 0;
            timeSinceLastTimeUpdate = -1L;
        }
    }

    private double getTickRateAverage() {
        synchronized (tickRates) {
            double sum = 0f;
            for (int i = 0; i < tickRates.length; i++) {
                sum += tickRates[i];
            }
            return sum / tickRates.length;
        }
    }

    public String getTPS() {
        return String.format("%.2f", getTickRateAverage());
    }
}
