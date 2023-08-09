package com.zenith.event.proxy;

import com.github.steveice10.mc.auth.data.GameProfile;

import java.net.SocketAddress;

public record NonWhitelistedPlayerConnectedEvent(GameProfile gameProfile, SocketAddress remoteAddress) {
}
