package com.zenith.event.proxy;

public class SelfDeathMessageEvent {
    public final String message;

    public SelfDeathMessageEvent(final String message) {
        this.message = message;
    }
}
