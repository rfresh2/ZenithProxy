package com.zenith.feature.ratelimiter;

import com.zenith.util.Timer;
import com.zenith.util.Timers;
import lombok.Getter;
import lombok.Setter;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.SERVER_LOG;

public class PacketRateLimiter {
    private final IntervalledCounter counter = new IntervalledCounter((long) (CONFIG.server.packetRateLimiter.intervalSeconds * 1.0e9));
    private final Timer timer = Timers.timer();
    @Setter @Getter
    private boolean active = true;

    // returns true if rate limit is exceeded
    public synchronized boolean countPacket() {
        if (!active) return false;
        counter.updateAndAdd(1, System.nanoTime());
        if (CONFIG.server.packetRateLimiter.logRate && timer.tick(5000)) {
            SERVER_LOG.info("[PacketRateLimiter] {}", getRate());
        }
        return CONFIG.server.packetRateLimiter.maxPacketsPerInterval <= counter.getRate();
    }

    public double getRate() {
        return counter.getRate();
    }
}
