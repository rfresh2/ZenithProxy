package com.zenith.event.proxy;

import com.github.steveice10.mc.auth.data.GameProfile;

public class ProxySpectatorConnectedEvent {
    public final GameProfile clientGameProfile;
    public ProxySpectatorConnectedEvent(final GameProfile clientGameProfile) {
        this.clientGameProfile = clientGameProfile;
    }
}
