package com.zenith.event.client;

/**
 * Tick emitted when no player is controlling the client
 * i.e. when the zenith "bot" handlers are controlling the player
 */
public record ClientBotTick() {
    /** Posted when the bot is ticked, after ClientTickEvent. **/
    public static final ClientBotTick INSTANCE = new ClientBotTick();

    public record Starting() {
        /** Posted when the bot starts ticking. Can occur when the client connects and no players are controlling **/
        public static final Starting INSTANCE = new Starting();
    }

    public record Stopped() {
        /** Posted when the bot stops ticking. Can occur if the client disconnects, or a controller logs in **/
        public static final Stopped INSTANCE = new Stopped();
    }
}
