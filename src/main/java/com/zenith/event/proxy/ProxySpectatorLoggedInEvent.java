package com.zenith.event.proxy;

import com.zenith.network.server.ServerConnection;

// the spectator has logged in and been sent the ClientboundLoginPacket
public record ProxySpectatorLoggedInEvent(ServerConnection session) { }
