package com.zenith.event.proxy;

import com.zenith.network.server.ServerConnection;

// triggered after GameProfile but before Login packet is sent
public record PlayerLoginEvent(ServerConnection serverConnection) { }
