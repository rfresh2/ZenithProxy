package com.zenith.cache;

public enum CacheResetType {
    FULL, // full disconnect
    PROTOCOL_SWITCH, // e.g. switch to configuration from play
    RESPAWN, // On ClientboundRespawnPacket
    LOGIN // on ClientboundLoginPacket. Velocity will also send this on backend server switches
}
