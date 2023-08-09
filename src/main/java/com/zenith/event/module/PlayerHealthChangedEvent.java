package com.zenith.event.module;

public class PlayerHealthChangedEvent {
    public final float previousHealth;
    public final float newHealth;

    public PlayerHealthChangedEvent(float newHealth, float previousHealth) {
        this.previousHealth = previousHealth;
        this.newHealth = newHealth;
    }
}
