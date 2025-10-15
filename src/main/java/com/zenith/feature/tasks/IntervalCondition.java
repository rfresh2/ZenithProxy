package com.zenith.feature.tasks;

import lombok.Data;
import org.jetbrains.annotations.ApiStatus;

import java.time.Duration;
import java.time.Instant;

/**
 * A {@link Condition} that is met after a specific {@link Duration} has passed since the last time it was met.
 */
@Data
@ApiStatus.Experimental
public class IntervalCondition implements Condition {
    private final Instant startTime;
    private final Duration interval;
    private Instant lastT;

    public IntervalCondition(Instant startTime, Duration interval) {
        this.startTime = startTime;
        this.interval = interval;
        this.lastT = startTime;
    }

    public static IntervalCondition daily(int n) {
        return daily(Instant.now(), n);
    }

    public static IntervalCondition daily(Instant start, int n) {
        return new IntervalCondition(start, Duration.ofDays(n));
    }

    public static IntervalCondition hourly(int n) {
        return hourly(Instant.now(), n);
    }

    public static IntervalCondition hourly(Instant start, int n) {
        return new IntervalCondition(start, Duration.ofHours(n));
    }

    public static IntervalCondition minutely(int n) {
        return minutely(Instant.now(), n);
    }

    public static IntervalCondition minutely(Instant start, int n) {
        return new IntervalCondition(start, Duration.ofMinutes(n));
    }

    public static IntervalCondition secondly(int n) {
        return secondly(Instant.now(), n);
    }

    public static IntervalCondition secondly(Instant start, int n) {
        return new IntervalCondition(start, Duration.ofSeconds(n));
    }

    public static IntervalCondition tickly(int n) {
        return tickly(Instant.now(), n);
    }

    public static IntervalCondition tickly(Instant start, int n) {
        return new IntervalCondition(start, Duration.ofMillis(50L * n));
    }

    @Override
    public boolean isMet() {
        Instant now = Instant.now();
        if (Duration.between(lastT, now).compareTo(interval) >= 0) {
            lastT = now;
            return true;
        }
        return false;
    }
}
