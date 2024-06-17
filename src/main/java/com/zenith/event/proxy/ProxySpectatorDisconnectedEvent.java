package com.zenith.event.proxy;

import org.geysermc.mcprotocollib.auth.GameProfile;

public record ProxySpectatorDisconnectedEvent(GameProfile clientGameProfile) { }
