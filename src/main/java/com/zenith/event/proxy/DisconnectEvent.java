package com.zenith.event.proxy;

import com.zenith.Shared;

public record DisconnectEvent(String reason, boolean manualDisconnect) {
    public DisconnectEvent(String reason) {
        this(reason, (Shared.MANUAL_DISCONNECT.equals(reason)));
    }
}
