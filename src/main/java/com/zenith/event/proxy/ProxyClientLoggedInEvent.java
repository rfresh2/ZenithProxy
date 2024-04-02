package com.zenith.event.proxy;

// the client has logged in and been sent the ClientboundLoginPacket
public record ProxyClientLoggedInEvent(com.zenith.network.server.ServerConnection session) { }
