package com.zenith.event.proxy;

import com.zenith.network.server.ServerSession;

public record ServerConnectionAddedEvent(ServerSession serverConnection) { }
