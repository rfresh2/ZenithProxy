package com.zenith.event.module;

import com.collarmc.pounce.EventInfo;
import com.collarmc.pounce.Preference;

@EventInfo(preference = Preference.DISPATCH)
public class PlayerHealthChangedEvent {
    public final float previousHealth;
    public final float newHealth;

    public PlayerHealthChangedEvent(float newHealth, float previousHealth) {
        this.previousHealth = previousHealth;
        this.newHealth = newHealth;
    }
}
