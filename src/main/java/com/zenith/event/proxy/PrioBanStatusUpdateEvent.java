package com.zenith.event.proxy;

import com.collarmc.pounce.EventInfo;
import com.collarmc.pounce.Preference;

@EventInfo(preference = Preference.POOL)
public class PrioBanStatusUpdateEvent {
    public final boolean prioBanned;

    public PrioBanStatusUpdateEvent(boolean prioBanned) {
        this.prioBanned = prioBanned;
    }
}
