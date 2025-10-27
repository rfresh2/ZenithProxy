package com.zenith.event.player;

import com.zenith.network.server.ServerSession;

public record PlayerConfigurationEvent() {
    /**
     * Fired when the player has entered the initial login -> configuration phase, on zenith having received the ServerboundLoginAcknowledgedPacket
     */
    public record Entered(ServerSession session) { }

    /**
     * Fired right before the client exits the initial configuration phase, right before zenith sends the ClientboundFinishConfigurationPacket
     */
    public record Exiting(ServerSession session) { }

    /**
     * Fired when the player has exited the configuration phase, on zenith receiving the ServerboundFinishConfigurationPacket
     */
    public record Exited(ServerSession session) { }
}
