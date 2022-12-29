package com.zenith.event.proxy;

import com.collarmc.pounce.EventInfo;
import com.collarmc.pounce.Preference;

@EventInfo(preference = Preference.POOL)
public class AutoReconnectEvent {
    public final int delaySeconds;

    public AutoReconnectEvent(final int delaySeconds) {
        this.delaySeconds = delaySeconds;
    }
}
