package com.zenith.feature.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The use state has to be driven locally: the server echoes it back in the entity metadata, but
 * only after a round trip, so reading that copy would make movement lag the server's slowdown by
 * the connection latency.
 */
public class UsingItemStateTest {

    @Test
    public void instantItemNeverEntersUseState() {
        final UsingItemState state = new UsingItemState();
        // blocks, buckets, ender pearls, ... finish in the same tick they are used
        state.start(UsingItemState.NOT_USING);
        assertFalse(state.isUsing());
    }

    @Test
    public void consumableStaysActiveForItsDuration() {
        final UsingItemState state = new UsingItemState();
        state.start(UsingItemState.consumeTicks(1.6f)); // a normal food item takes 32 ticks
        assertTrue(state.isUsing());

        for (int i = 0; i < 31; i++) {
            state.tick();
            assertTrue(state.isUsing(), "use must stay active for the full duration");
        }
        state.tick();
        assertFalse(state.isUsing(), "use must end once the duration elapses");
    }

    @Test
    public void consumedInstantFoodStaysActiveAtLeastOneTick() {
        // vanilla still slows for the tick the use was started on
        assertEquals(1, UsingItemState.consumeTicks(0.0f));
    }

    @Test
    public void heldOpenUseOnlyEndsWhenReleased() {
        final UsingItemState state = new UsingItemState();
        state.start(UsingItemState.HELD_OPEN);
        assertTrue(state.isUsing());

        for (int i = 0; i < 200; i++) {
            state.tick();
        }
        assertTrue(state.isUsing(), "a bow or shield stays in use until it is released");
        assertEquals(UsingItemState.HELD_OPEN, state.ticksRemaining());

        state.stop();
        assertFalse(state.isUsing());
    }

    @Test
    public void consumeTicksRoundUp() {
        assertEquals(32, UsingItemState.consumeTicks(1.6f));
        assertEquals(20, UsingItemState.consumeTicks(1.0f));
        // a shorter-than-a-tick use still occupies the tick it started on
        assertEquals(1, UsingItemState.consumeTicks(0.01f));
    }

    @Test
    public void restartReplacesPreviousUse() {
        final UsingItemState state = new UsingItemState();
        state.start(UsingItemState.HELD_OPEN);
        state.start(UsingItemState.consumeTicks(1.0f));

        state.tick();
        assertTrue(state.isUsing());
        assertEquals(19, state.ticksRemaining());
    }

    @Test
    public void stopIsIdempotent() {
        // releasing when nothing is in use must not throw or corrupt the next use
        final UsingItemState state = new UsingItemState();
        state.stop();
        assertFalse(state.isUsing());

        state.start(UsingItemState.consumeTicks(1.0f));
        state.stop();
        state.stop();
        assertFalse(state.isUsing());

        state.start(UsingItemState.consumeTicks(1.0f));
        assertTrue(state.isUsing(), "a new use after stop must be tracked");
    }
}
