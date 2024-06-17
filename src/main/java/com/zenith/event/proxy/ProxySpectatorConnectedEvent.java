package com.zenith.event.proxy;

import org.geysermc.mcprotocollib.auth.GameProfile;

public record ProxySpectatorConnectedEvent(GameProfile clientGameProfile) { }
