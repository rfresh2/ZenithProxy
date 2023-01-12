package com.zenith.event.proxy;

import com.zenith.util.Constants;

public class DisconnectEvent {
    public final String reason;
    public final boolean manualDisconnect;

    public DisconnectEvent(String reason) {
        this.reason = reason;
        this.manualDisconnect = (Constants.MANUAL_DISCONNECT.equals(reason));
    }
}
