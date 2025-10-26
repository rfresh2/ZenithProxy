package com.zenith.feature.tps;

import com.google.common.primitives.Doubles;
import com.zenith.Proxy;
import com.zenith.event.client.ClientConnectEvent;
import com.zenith.event.client.ClientTickEvent;
import com.zenith.util.struct.CircularFifoQueue;
import lombok.Locked;

import java.time.Duration;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.EVENT_BUS;

public class TPSCalculator {

    private final CircularFifoQueue<Double> tickRates;
    private long prevWorldTimeUpdate = -1;

    /**
     * Instances of this class will never be garbage collected automatically
     *
     * If you want to dispose of it, you must unsubscribe it from the event bus manually
     */
    public TPSCalculator(int tpsBufferSize) {
        tickRates = new CircularFifoQueue<>(tpsBufferSize);
        EVENT_BUS.subscribe(this,
            of(ClientConnectEvent.class, (e) -> reset()),
            of(ClientTickEvent.class, this::onTick)
        );
    }

    @Locked
    private void onTick(ClientTickEvent event) {
        if (!Proxy.getInstance().isOnlineForAtLeastDuration(Duration.ofSeconds(1))) return;
        var worldTimeData = CACHE.getChunkCache().getWorldTimeData();
        if (worldTimeData == null) return;
        long lastUpdate = worldTimeData.getLastUpdate();
        if (prevWorldTimeUpdate == -1) {
            prevWorldTimeUpdate = lastUpdate;
            return;
        }
        if (prevWorldTimeUpdate == lastUpdate) return;
        double timeElapsed = lastUpdate - prevWorldTimeUpdate;
        double tps = Doubles.constrainToRange(20.0 / (timeElapsed / 1000.0), 0.0, 20.0);
        tickRates.add(tps);
        prevWorldTimeUpdate = lastUpdate;
    }

    @Locked
    public void reset() {
        tickRates.clear();
        prevWorldTimeUpdate = -1;
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
     * Returns the current average server TPS as a double value in the range [0.0, 20.0].
     * Falls back to 20.0 when insufficient data has been collected yet.
     */
    public double getTPSValue() {
        return getTickRateAverage();
    }
}
