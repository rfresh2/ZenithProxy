package com.zenith.event.proxy;

public class PrioBanStatusUpdateEvent {
    public final boolean prioBanned;

    public PrioBanStatusUpdateEvent(boolean prioBanned) {
        this.prioBanned = prioBanned;
    }
}
