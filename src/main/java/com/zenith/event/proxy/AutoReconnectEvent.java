package com.zenith.event.proxy;

public class AutoReconnectEvent {
    public final int delaySeconds;

    public AutoReconnectEvent(final int delaySeconds) {
        this.delaySeconds = delaySeconds;
    }
}
