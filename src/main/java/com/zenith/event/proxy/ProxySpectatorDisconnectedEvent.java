package com.zenith.event.proxy;

import com.github.steveice10.mc.auth.data.GameProfile;

public class ProxySpectatorDisconnectedEvent {
    public final GameProfile clientGameProfile;

    public ProxySpectatorDisconnectedEvent(final GameProfile clientGameProfile) {
        this.clientGameProfile = clientGameProfile;
    }
}
