package com.zenith.event.proxy;

import com.zenith.network.server.ServerSession;

// triggered after GameProfile but before Login packet is sent
public record PlayerLoginEvent(ServerSession serverConnection) { }
