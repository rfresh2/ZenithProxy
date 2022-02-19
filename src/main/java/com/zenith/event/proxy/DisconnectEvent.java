package com.zenith.event.proxy;

import com.collarmc.pounce.EventInfo;
import com.collarmc.pounce.Preference;
import com.zenith.util.Constants;

@EventInfo(preference = Preference.POOL)
public class DisconnectEvent {
    public final String reason;
    public final boolean manualDisconnect;

    public DisconnectEvent(String reason) {
        this.reason = reason;
        this.manualDisconnect = (Constants.MANUAL_DISCONNECT.equals(reason));
    }
}
