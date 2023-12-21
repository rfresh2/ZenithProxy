package com.zenith.event.proxy;

import com.zenith.Shared;

import java.time.Duration;

public record DisconnectEvent(String reason, boolean manualDisconnect, Duration onlineDuration) {
    public DisconnectEvent(String reason, final Duration onlineDuration) {
        this(reason, (Shared.MANUAL_DISCONNECT.equals(reason)), onlineDuration);
    }

    public DisconnectEvent(String reason) {
        this(reason, Duration.ZERO);
    }
}
