package com.zenith.event.proxy;

import com.collarmc.pounce.EventInfo;
import com.collarmc.pounce.Preference;

@EventInfo(preference = Preference.POOL)
public class ProxyClientDisconnectedEvent {
    public String message;
    public ProxyClientDisconnectedEvent() {
    }

    public ProxyClientDisconnectedEvent(final String message) {
        this.message = message;
    }
}
