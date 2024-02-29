package com.zenith.event.module;

// Tick that is constantly emitted even when a user is connected to the proxy
// This is not necessarily tied to 50ms like the normal client tick, just to save on resources while its not needed
public record ClientOnlineTickEvent() {
    public static ClientOnlineTickEvent INSTANCE = new ClientOnlineTickEvent();
    public record Starting() {
        public static ClientOnlineTickEvent.Starting INSTANCE = new ClientOnlineTickEvent.Starting();
    }

    public record Stopped() {
        public static ClientOnlineTickEvent.Stopped INSTANCE = new ClientOnlineTickEvent.Stopped();
    }
}
