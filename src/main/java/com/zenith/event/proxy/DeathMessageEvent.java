package com.zenith.event.proxy;

import com.collarmc.pounce.EventInfo;
import com.collarmc.pounce.Preference;

@EventInfo(preference = Preference.POOL)
public class DeathMessageEvent {
    public final String message;

    public DeathMessageEvent(final String message) {
        this.message = message;
    }
}
