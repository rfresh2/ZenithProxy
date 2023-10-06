package com.zenith.event.module;

public record ClientTickEvent() {
    public static final ClientTickEvent INSTANCE = new ClientTickEvent();
}
