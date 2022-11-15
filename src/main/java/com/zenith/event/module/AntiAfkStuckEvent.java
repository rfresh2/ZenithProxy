package com.zenith.event.module;

import com.collarmc.pounce.EventInfo;
import com.collarmc.pounce.Preference;

@EventInfo(preference = Preference.POOL)
public class AntiAfkStuckEvent {
    public final double distanceMovedDelta;

    public AntiAfkStuckEvent(final double distanceMovedDelta) {
        this.distanceMovedDelta = distanceMovedDelta;
    }
}
