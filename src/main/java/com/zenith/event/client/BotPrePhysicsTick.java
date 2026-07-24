package com.zenith.event.client;

// Posted once per bot tick, after the bot has applied the server's latest entity metadata sync
// (e.g. fall-flying/pose/abilities) for this tick, but before the bot's local physics/movement step runs.
// Intended for plugins that need to influence tick-local physics-affecting state (e.g. fall-flying) at this
// exact point, so that client-side prediction stays consistent with what the server confirms a moment later.
public record BotPrePhysicsTick() {
    public static final BotPrePhysicsTick INSTANCE = new BotPrePhysicsTick();
}
