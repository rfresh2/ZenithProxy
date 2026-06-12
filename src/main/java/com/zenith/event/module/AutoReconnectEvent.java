package com.zenith.event.module;

/**
 * Fired when AutoReconnect has decided to reconnect, and the scheduled connect countdown has begun
 * @param delaySeconds time until the reconnect will occur
 */
public record AutoReconnectEvent(int delaySeconds) { }
