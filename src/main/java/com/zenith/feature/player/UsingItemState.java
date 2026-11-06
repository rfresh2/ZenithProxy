package com.zenith.feature.player;

/**
 * Client side model of the vanilla "using an item" state.
 *
 * <p>The server mirrors this state back in the living entity metadata, but that copy only arrives
 * after a full round trip, so it lags behind by however many ticks the connection latency spans.
 * Vanilla clients do not wait for it: they enter the use state the moment they send the use
 * packet and leave it the moment the use ends, which is what this class reproduces.
 *
 * <p>Movement reads this state because vanilla multiplies both movement impulses by 0.2 while it
 * is active.
 */
public final class UsingItemState {
    /** The use stays active until it is explicitly released (bow, shield, trident, ...). */
    public static final int HELD_OPEN = -1;
    /** The item never enters the use state at all. */
    public static final int NOT_USING = 0;

    private boolean using;
    private int ticksRemaining;

    public boolean isUsing() {
        return this.using;
    }

    /**
     * Starts a use.
     *
     * @param useTicks {@link #HELD_OPEN} for a use that must be released, {@link #NOT_USING} for an
     *                 instant item, or the number of ticks a self finishing use stays active
     */
    public void start(final int useTicks) {
        if (useTicks == NOT_USING) {
            stop();
            return;
        }
        this.using = true;
        this.ticksRemaining = useTicks;
    }

    public void stop() {
        this.using = false;
        this.ticksRemaining = 0;
    }

    /**
     * Advances by one tick, ending a self finishing use when it runs out.
     */
    public void tick() {
        if (!this.using) return;
        if (this.ticksRemaining > 0 && --this.ticksRemaining <= 0) {
            stop();
        }
    }

    /** Remaining ticks of a self finishing use, or {@link #HELD_OPEN} while one is held open. */
    public int ticksRemaining() {
        return this.ticksRemaining;
    }

    /**
     * Converts a consumable duration to the number of ticks vanilla keeps the use active.
     */
    public static int consumeTicks(final float consumeSeconds) {
        return Math.max(1, (int) Math.ceil(consumeSeconds * 20.0f));
    }
}
