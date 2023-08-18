package com.zenith.event.module;

public class AntiAfkStuckEvent {
    public final double distanceMovedDelta;

    public AntiAfkStuckEvent(final double distanceMovedDelta) {
        this.distanceMovedDelta = distanceMovedDelta;
    }
}
