package com.zenith.feature.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The movement impulse modifiers vanilla applies in {@code LocalPlayer#aiStep}.
 *
 * <p>The order matters and is the part worth pinning down: sneaking scales by the sneaking speed
 * attribute, using an item scales by 0.2, and the 0.98 input factor is applied last. Applying the
 * use multiplier to an already scaled value, or skipping the final scale, produces a movement
 * vector the server never predicts.
 */
public class BotMovementInputTest {

    private static final float EPSILON = 1.0E-6f;
    private static final float VANILLA_SNEAK_SPEED = 0.3f;

    @Test
    public void plainInputIsOnlyScaledByTheInputFactor() {
        assertEquals(0.98f, Bot.applyMovementModifiers(1.0f, false, VANILLA_SNEAK_SPEED, false), EPSILON);
        assertEquals(-0.98f, Bot.applyMovementModifiers(-1.0f, false, VANILLA_SNEAK_SPEED, false), EPSILON);
        assertEquals(0.0f, Bot.applyMovementModifiers(0.0f, false, VANILLA_SNEAK_SPEED, false), EPSILON);
    }

    @Test
    public void usingAnItemScalesTheInputByPointTwo() {
        assertEquals(0.2f * 0.98f, Bot.applyMovementModifiers(1.0f, false, VANILLA_SNEAK_SPEED, true), EPSILON);
        assertEquals(-0.2f * 0.98f, Bot.applyMovementModifiers(-1.0f, false, VANILLA_SNEAK_SPEED, true), EPSILON);
    }

    @Test
    public void sneakingScalesByTheSneakAttribute() {
        assertEquals(VANILLA_SNEAK_SPEED * 0.98f,
            Bot.applyMovementModifiers(1.0f, true, VANILLA_SNEAK_SPEED, false), EPSILON);
    }

    @Test
    public void sneakingWhileUsingAnItemCompoundsBothModifiers() {
        // eating while crouched: 0.3 * 0.2 * 0.98, and crucially not 0.3 * 0.98 * 0.2 applied twice
        // or the use multiplier applied to the raw 1.0
        assertEquals(VANILLA_SNEAK_SPEED * 0.2f * 0.98f,
            Bot.applyMovementModifiers(1.0f, true, VANILLA_SNEAK_SPEED, true), EPSILON);
    }

    @Test
    public void useMultiplierFollowsTheSneakSpeedAttribute() {
        // a modified sneaking speed attribute (e.g. a swift sneak enchantment) has to keep flowing
        // through instead of being replaced by a hardcoded 0.3
        final float customSneakSpeed = 0.75f;
        assertEquals(customSneakSpeed * 0.2f * 0.98f,
            Bot.applyMovementModifiers(1.0f, true, customSneakSpeed, true), EPSILON);
    }

    @Test
    public void zeroInputStaysZeroUnderEveryModifier() {
        assertEquals(0.0f, Bot.applyMovementModifiers(0.0f, true, VANILLA_SNEAK_SPEED, true), EPSILON);
    }
}
