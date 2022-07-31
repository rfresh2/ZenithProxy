package com.zenith.event.proxy;

import com.collarmc.pounce.EventInfo;
import com.collarmc.pounce.Preference;

@EventInfo(preference = Preference.POOL)
public class PrioStatusUpdateEvent {
    public final boolean prio;

    public PrioStatusUpdateEvent(boolean prio) {
        this.prio = prio;
    }
}
