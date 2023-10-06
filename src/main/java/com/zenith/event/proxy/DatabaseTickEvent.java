package com.zenith.event.proxy;

// Constant tick when we are connected on 2b2t every 5 minutes
public record DatabaseTickEvent() {
    public static final DatabaseTickEvent INSTANCE = new DatabaseTickEvent();
}
