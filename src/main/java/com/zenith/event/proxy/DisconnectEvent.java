package com.zenith.event.proxy;

import com.zenith.Shared;

public class DisconnectEvent {
    public final String reason;
    public final boolean manualDisconnect;

    public DisconnectEvent(String reason) {
        this.reason = reason;
        this.manualDisconnect = (Shared.MANUAL_DISCONNECT.equals(reason));
    }
}
