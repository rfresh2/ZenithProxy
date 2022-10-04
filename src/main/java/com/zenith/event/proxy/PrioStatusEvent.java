package com.zenith.event.proxy;

import com.collarmc.pounce.EventInfo;
import com.collarmc.pounce.Preference;

@EventInfo(preference = Preference.POOL)
public class PrioStatusEvent {
    public final boolean prio;

    public PrioStatusEvent(boolean prio) {
        this.prio = prio;
    }
}
