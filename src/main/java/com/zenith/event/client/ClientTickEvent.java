package com.zenith.event.client;

public record ClientTickEvent() {
    /**
     * Ticks emitted while the client is online. Posted before ClientBotTick
     */
    public static final ClientTickEvent INSTANCE = new ClientTickEvent();
    public record End() {
        /** Posted at the end of the client tick, after ClientBotTick **/
        public static final ClientTickEvent.End INSTANCE = new ClientTickEvent.End();
    }
    public record Starting() {
        /** Posted when client ticks are beginning. When the client has connected. **/
        public static final ClientTickEvent.Starting INSTANCE = new ClientTickEvent.Starting();
    }
    public record Stopped() {
        /** Posted when client ticks are stopping. When the client has disconnected. **/
        public static final ClientTickEvent.Stopped INSTANCE = new ClientTickEvent.Stopped();
    }

}
