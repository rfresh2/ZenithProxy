package com.zenith.event.module;

public record ClientTickEvent() {
    /**
     * Ticks emitted while the client is online
     */
    public static final ClientTickEvent INSTANCE = new ClientTickEvent();
    public record Starting() {
        public static final ClientTickEvent.Starting INSTANCE = new ClientTickEvent.Starting();
    }
    public record Stopped() {
        public static final ClientTickEvent.Stopped INSTANCE = new ClientTickEvent.Stopped();
    }

}
