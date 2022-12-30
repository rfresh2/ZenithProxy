package com.zenith.event.proxy;

import com.collarmc.pounce.EventInfo;
import com.collarmc.pounce.Preference;

@EventInfo(preference = Preference.POOL)
public class SelfDeathMessageEvent {
    public final String message;

    public SelfDeathMessageEvent(final String message) {
        this.message = message;
    }
}
