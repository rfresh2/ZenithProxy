package com.zenith.event.proxy;

import com.github.steveice10.mc.auth.data.GameProfile;

public class ProxyClientDisconnectedEvent {
    public GameProfile clientGameProfile;
    public String reason;
    public ProxyClientDisconnectedEvent() {
    }

    public ProxyClientDisconnectedEvent(final String reason) {
        this.reason = reason;
    }

    public ProxyClientDisconnectedEvent(final String reason, final GameProfile gameProfile) {
        this.reason = reason;
        this.clientGameProfile = gameProfile;
    }
}
