package com.zenith.event.proxy;

import com.collarmc.pounce.EventInfo;
import com.collarmc.pounce.Preference;
import com.github.steveice10.mc.auth.data.GameProfile;

@EventInfo(preference = Preference.POOL)
public class ProxySpectatorConnectedEvent {
    public final GameProfile clientGameProfile;
    public ProxySpectatorConnectedEvent(final GameProfile clientGameProfile) {
        this.clientGameProfile = clientGameProfile;
    }
}
