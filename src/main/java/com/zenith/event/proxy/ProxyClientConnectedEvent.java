package com.zenith.event.proxy;

import com.collarmc.pounce.EventInfo;
import com.collarmc.pounce.Preference;
import com.github.steveice10.mc.auth.data.GameProfile;

@EventInfo(preference = Preference.POOL)
public class ProxyClientConnectedEvent {
    public final GameProfile clientGameProfile;
    // if this client is the active player or is a spectator
    public boolean isPlayer;
    public ProxyClientConnectedEvent(final GameProfile clientGameProfile, final boolean isPlayer) {
        this.clientGameProfile = clientGameProfile;
        this.isPlayer = isPlayer;
    }
}
