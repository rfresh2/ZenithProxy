package com.zenith.event.system;

/**
 * Posted when ZenithProxy has completed launching
 */
public record ProxyLaunchedEvent() {
    public static final ProxyLaunchedEvent INSTANCE = new ProxyLaunchedEvent();
}
