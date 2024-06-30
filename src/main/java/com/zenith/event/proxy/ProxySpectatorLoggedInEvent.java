package com.zenith.event.proxy;

import com.zenith.network.server.ServerSession;

// the spectator has logged in and been sent the ClientboundLoginPacket
public record ProxySpectatorLoggedInEvent(ServerSession session) { }
