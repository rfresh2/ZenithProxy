package com.zenith.event.proxy;

import org.geysermc.mcprotocollib.auth.GameProfile;

public record ProxyClientDisconnectedEvent(String reason, GameProfile clientGameProfile) {

    public ProxyClientDisconnectedEvent() {
        this(null, null);
    }

    public ProxyClientDisconnectedEvent(final String reason) {
        this(reason, null);
    }
}
