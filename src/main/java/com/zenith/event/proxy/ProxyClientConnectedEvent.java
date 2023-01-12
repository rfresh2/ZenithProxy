package com.zenith.event.proxy;

import com.github.steveice10.mc.auth.data.GameProfile;


public class ProxyClientConnectedEvent {
    public final GameProfile clientGameProfile;
    public ProxyClientConnectedEvent(final GameProfile clientGameProfile) {
        this.clientGameProfile = clientGameProfile;
    }
}
