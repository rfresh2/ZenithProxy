package com.zenith.feature.tps;

import com.zenith.event.client.ClientTickEvent;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.EVENT_BUS;

/**
 * Instances of this class will not be garbage collected by default
 *
 * If you want to dispose of it, you must unsubscribe it from the event bus manually
 */
public class ClientTPSCalculator extends BaseTPSCalculator {
    private long prevTick = -1;

    public ClientTPSCalculator(int tpsBufferSize) {
        super(tpsBufferSize);
        EVENT_BUS.subscribe(this,
            of(ClientTickEvent.class, Integer.MAX_VALUE - 10, this::onTick),
            of(ClientTickEvent.Starting.class, (e) -> reset()),
            of(ClientTickEvent.Stopped.class, (e) -> reset())
        );
    }

    private void onTick(ClientTickEvent event) {
        var now = System.nanoTime();
        if (prevTick == -1) {
            prevTick = now;
            return;
        }
        var nsElapsed = now - prevTick;
        prevTick = now;
        recordTick(nsElapsed, 1.0);
    }

    @Override
    public void reset() {
        super.reset();
        prevTick = -1;
    }
}
