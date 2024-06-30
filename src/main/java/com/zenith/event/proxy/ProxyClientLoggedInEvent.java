package com.zenith.event.proxy;

import com.zenith.network.server.ServerSession;

// the client has logged in and been sent the ClientboundLoginPacket
public record ProxyClientLoggedInEvent(ServerSession session) { }
