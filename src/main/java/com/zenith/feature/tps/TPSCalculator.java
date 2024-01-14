package com.zenith.feature.tps;

import com.google.common.primitives.Floats;
import org.apache.commons.collections4.queue.CircularFifoQueue;

public class TPSCalculator {

    /**
     * Calculator credits to Lambda
     */

    private static final int TICK_RATES_SIZE = 120;
    private final CircularFifoQueue<Float> tickRates;
    private Long timeSinceLastTimeUpdate = -1L;

    public TPSCalculator() {
        this.tickRates = new CircularFifoQueue<>(TICK_RATES_SIZE);
        reset();
    }

    public void handleTimeUpdate() {
        if (this.timeSinceLastTimeUpdate != -1L) {
            final double timeElapsed = (System.nanoTime() - timeSinceLastTimeUpdate) / 1E9;
            final float tps = Floats.constrainToRange((float) (20.0 / timeElapsed), 0.0f, 20.0f);
            synchronized (this.tickRates) {
                this.tickRates.add(tps);
            }
        }
        this.timeSinceLastTimeUpdate = System.nanoTime();
    }

    public void reset() {
        synchronized (this.tickRates) {
            // fill with 20.0 tps by default
            for (int i = 0; i < TICK_RATES_SIZE; i++) {
                this.tickRates.add(20.0f);
            }
            this.timeSinceLastTimeUpdate = -1L;
        }
    }

    private Float getTickRatesAverage() {
        synchronized (this.tickRates) {
            if (this.tickRates.isEmpty()) return 0.0f;
            return this.tickRates.stream()
                    .reduce(Float::sum)
                    .map(sum -> sum / this.tickRates.size())
                    .orElse(0.0f);
        }
    }

    public String getTPS() {
        return String.format("%.2f", getTickRatesAverage());
    }
}
