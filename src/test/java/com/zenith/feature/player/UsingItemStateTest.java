package com.zenith.feature.player;

import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
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

    private static final int BOW_ID = 832;
    private static final int APPLE_ID = 831;

    @Test
    public void instantItemNeverEntersUseState() {
        final UsingItemState state = new UsingItemState();
        // blocks, buckets, ender pearls, ... finish in the same tick they are used
        state.start(UsingItemState.NOT_USING, Hand.MAIN_HAND, APPLE_ID);
        assertFalse(state.isUsing());
    }

    @Test
    public void consumableStaysActiveForItsDuration() {
        final UsingItemState state = new UsingItemState();
        state.start(UsingItemState.consumeTicks(1.6f), Hand.MAIN_HAND, APPLE_ID); // 32 ticks
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
        state.start(UsingItemState.HELD_OPEN, Hand.MAIN_HAND, BOW_ID);
        assertTrue(state.isUsing());
        assertTrue(state.isHeldOpen());

        for (int i = 0; i < 200; i++) {
            state.tick();
        }
        assertTrue(state.isUsing(), "a bow or shield stays in use until it is released");
        assertEquals(UsingItemState.HELD_OPEN, state.ticksRemaining());

        state.stop();
        assertFalse(state.isUsing());
        assertFalse(state.isHeldOpen());
    }

    @Test
    public void finiteUseIsNotReportedAsHeldOpen() {
        final UsingItemState state = new UsingItemState();
        state.start(UsingItemState.consumeTicks(1.0f), Hand.MAIN_HAND, APPLE_ID);
        assertFalse(state.isHeldOpen(), "an eating use finishes on its own");
    }

    @Test
    public void startedHandAndItemAreRemembered() {
        // movement only needs isUsing(), but releasing a held open use needs to know which item
        // the use belongs to, otherwise a slot change would be invisible to it
        final UsingItemState state = new UsingItemState();
        state.start(UsingItemState.HELD_OPEN, Hand.OFF_HAND, BOW_ID);
        assertEquals(Hand.OFF_HAND, state.hand());
        assertEquals(BOW_ID, state.itemId());

        state.stop();
        assertEquals(-1, state.itemId(), "a stopped use must not claim an item");
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
        state.start(UsingItemState.HELD_OPEN, Hand.MAIN_HAND, BOW_ID);
        state.start(UsingItemState.consumeTicks(1.0f), Hand.MAIN_HAND, APPLE_ID);

        state.tick();
        assertTrue(state.isUsing());
        assertEquals(19, state.ticksRemaining());
        assertEquals(APPLE_ID, state.itemId(), "the newer use must win");
    }

    @Test
    public void stopIsIdempotent() {
        // releasing when nothing is in use must not throw or corrupt the next use
        final UsingItemState state = new UsingItemState();
        state.stop();
        assertFalse(state.isUsing());

        state.start(UsingItemState.consumeTicks(1.0f), Hand.MAIN_HAND, APPLE_ID);
        state.stop();
        state.stop();
        assertFalse(state.isUsing());

        state.start(UsingItemState.consumeTicks(1.0f), Hand.MAIN_HAND, APPLE_ID);
        assertTrue(state.isUsing(), "a new use after stop must be tracked");
    }
}
