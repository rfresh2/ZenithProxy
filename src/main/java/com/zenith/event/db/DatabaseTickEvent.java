package com.zenith.event.db;

// Constant tick when we are connected on 2b2t every minute
public record DatabaseTickEvent() {
    public static final int TICK_INTERVAL_SECONDS = 60;
    public static final DatabaseTickEvent INSTANCE = new DatabaseTickEvent();
}
