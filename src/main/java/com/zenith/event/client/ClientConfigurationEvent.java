package com.zenith.event.client;

public record ClientConfigurationEvent() {
    /**
     * Fired right before the client enters the configuration phase, on receiving the ClientboundStartConfigurationPacket
     *
     * This does *not* get fired on initial login->configuration state switch, only when reconfiguring
     */
    public record Entering() {
        public static final ClientConfigurationEvent.Entering INSTANCE = new ClientConfigurationEvent.Entering();
    }

    /**
     * Fired when the client has entered the configuration phase, on having sent the ServerboundConfigurationAcknowledgedPacket
     */
    public record Entered() {
        public static final ClientConfigurationEvent.Entered INSTANCE = new ClientConfigurationEvent.Entered();
    }

    /**
     * Fired right before the client exits the configuration phase, on receiving the ClientboundFinishConfigurationPacket
     */
    public record Exiting() {
        public static final ClientConfigurationEvent.Exiting INSTANCE = new ClientConfigurationEvent.Exiting();
    }

    /**
     * Fired when the client has exited the configuration phase, on having sent the ServerboundFinishConfigurationPacket
     */
    public record Exited() {
        public static final ClientConfigurationEvent.Exited INSTANCE = new ClientConfigurationEvent.Exited();
    }
}
