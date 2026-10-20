package com.zenith.feature.tps;

import com.zenith.util.struct.CircularFifoQueue;
import lombok.Locked;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class BaseTPSCalculator {
    private final CircularFifoQueue<Double> tickRates;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public BaseTPSCalculator(int tpsBufferSize) {
        tickRates = new CircularFifoQueue<>(tpsBufferSize);
    }

    /**
     * record a tick event
     * @param expectedTicksBetween expected number of ticks between recorded ticks
     */
    @Locked
    public void recordTick(long nsElapsed, double expectedTicksBetween) {
        double tps = expectedTicksBetween / (nsElapsed / (1000.0 * 1000.0 * 1000.0));
        tickRates.add(tps);
    }

    @Locked
    public void reset() {
        tickRates.clear();
    }

    @Locked
    private double getTickRateAverage() {
        if (tickRates.isEmpty()) return 20.0;
        double sum = 0f;
        for (var d : tickRates) {
            sum += d;
        }
        return sum / tickRates.size();
    }

    public String getTPS() {
        return String.format("%.2f", getTickRateAverage());
    }

    /**
     * Returns the current average server TPS as a double value
     * Falls back to 20.0 when insufficient data has been collected yet.
     */
    public double getTPSValue() {
        return getTickRateAverage();
    }
}
