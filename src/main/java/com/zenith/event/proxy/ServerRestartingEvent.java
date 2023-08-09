package com.zenith.event.proxy;

public class ServerRestartingEvent {
    public final String message;

    public ServerRestartingEvent(String message) { this.message = message; }
}
