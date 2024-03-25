package com.zenith.event.module;

// Tick emitted when no player is controlling the client
// i.e. when the zenith "bot" handlers are controlling the player
public record ClientBotTick() {
    public static final ClientBotTick INSTANCE = new ClientBotTick();

    public record Starting() {
        public static final Starting INSTANCE = new Starting();
    }

    public record Stopped() {
        public static final Stopped INSTANCE = new Stopped();
    }
}
