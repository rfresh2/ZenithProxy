package com.zenith.event.proxy;

public class PrioStatusUpdateEvent {
    public final boolean prio;

    public PrioStatusUpdateEvent(boolean prio) {
        this.prio = prio;
    }
}
