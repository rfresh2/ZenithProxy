package com.zenith.event.proxy;

import com.collarmc.pounce.EventInfo;
import com.collarmc.pounce.Preference;
import com.github.steveice10.mc.auth.data.GameProfile;

import java.net.SocketAddress;

@EventInfo(preference = Preference.POOL)
public record NonWhitelistedPlayerConnectedEvent(GameProfile gameProfile, SocketAddress remoteAddress) {
}
