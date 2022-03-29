package com.zenith.event.proxy;

import com.collarmc.pounce.EventInfo;
import com.collarmc.pounce.Preference;

@EventInfo(preference = Preference.POOL)
public class DeathEvent {
    public final String message;

    public DeathEvent(final String message) {
        this.message = message;
    }
}
