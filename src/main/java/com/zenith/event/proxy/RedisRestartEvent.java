package com.zenith.event.proxy;

public record RedisRestartEvent() {
    public static final RedisRestartEvent INSTANCE = new RedisRestartEvent();
}
