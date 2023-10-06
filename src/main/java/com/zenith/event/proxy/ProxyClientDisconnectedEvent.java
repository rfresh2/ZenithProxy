package com.zenith.event.proxy;

import com.github.steveice10.mc.auth.data.GameProfile;

public record ProxyClientDisconnectedEvent(String reason, GameProfile clientGameProfile) {

    public ProxyClientDisconnectedEvent() {
        this(null, null);
    }

    public ProxyClientDisconnectedEvent(final String reason) {
        this(reason, null);
    }
}
