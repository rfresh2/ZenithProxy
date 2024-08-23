package com.zenith;

import com.zenith.util.math.MathHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class YawRotationTest {

    @Test
    public void yawInRangeTest() {
        assertTrue(MathHelper.isYawInRange(0.0f, -180f, 180.0f));
        assertFalse(MathHelper.isYawInRange(0.0f, -180f, 179.9f));
        assertTrue(MathHelper.isYawInRange(0.0f, 185, 175.0f));
        assertFalse(MathHelper.isYawInRange(0.0f, 185, 174.9f));
        assertTrue(MathHelper.isYawInRange(0.0f, -185, 175.0f));
        assertFalse(MathHelper.isYawInRange(0.0f, -185, 174.9f));
        assertTrue(MathHelper.isYawInRange(-180f, 185, 5.0f));
        assertFalse(MathHelper.isYawInRange(-180f, 185, 4.0f));
        assertTrue(MathHelper.isYawInRange(185, -185, 10.0f));
        assertFalse(MathHelper.isYawInRange(185, -185, 9.0f));
    }

    @Test
    public void pitchInRangeTest() {
        assertTrue(MathHelper.isPitchInRange(0.0f, -180f, 180.0f));
        assertFalse(MathHelper.isPitchInRange(0.0f, -180f, 179.9f));
        assertTrue(MathHelper.isPitchInRange(0.0f, 185, 175.0f));
        assertFalse(MathHelper.isPitchInRange(0.0f, 185, 174.9f));
        assertTrue(MathHelper.isPitchInRange(0.0f, -185, 175.0f));
        assertFalse(MathHelper.isPitchInRange(0.0f, -185, 174.9f));
        assertTrue(MathHelper.isPitchInRange(-180f, 185, 5.0f));
        assertFalse(MathHelper.isPitchInRange(-180f, 185, 4.0f));
        assertTrue(MathHelper.isPitchInRange(185, -185, 10.0f));
        assertFalse(MathHelper.isPitchInRange(185, -185, 9.0f));
    }
}
