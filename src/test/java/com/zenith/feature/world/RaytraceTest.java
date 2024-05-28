package com.zenith.feature.world;

import com.zenith.mc.block.LocalizedCollisionBox;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RaytraceTest {
    @Test
    public void noIntersectionTestRealData() {

        final LocalizedCollisionBox cb = new LocalizedCollisionBox(-602.8, -602.2, 135.0, 136.95, -341.9, -341.3, -602.5, 135.0, -341.6);
        double x1 = -602.37;
        double x2 = -602.35;
        double y1 = 136.6;
        double y2 = 136.4;
        double z1 = -351.92;
        double z2 = -347.42;
        var intersection = cb.rayIntersection(x1, y1, z1, x2, y2, z2);

        Assertions.assertNull(intersection);
    }

    @Test
    public void failsWithLargeZ1toZ2Diff() {

        final LocalizedCollisionBox cb = new LocalizedCollisionBox(2.2, 2.8, 5.0, 6.95, 41.3, 41.9, 2.5, 5.0, 41.6);
        double x1 = 2.37;
        double x2 = 2.35;
        double y1 = 6.6;
        double y2 = 6.4;
        double z1 = 51.92;
        double z2 = 47.42;
        var intersection = cb.rayIntersection(x1, y1, z1, x2, y2, z2);

        Assertions.assertNull(intersection);
    }

    @Test
    public void passesWithSmallZ1toZ2Diff() {

        final LocalizedCollisionBox cb = new LocalizedCollisionBox(2.2, 2.8, 5.0, 6.95, 41.3, 41.9, 2.5, 5.0, 41.6);
        double x1 = 2.37;
        double x2 = 2.35;
        double y1 = 6.6;
        double y2 = 6.4;
        double z1 = 51.92;
        double z2 = 51.42;
        var intersection = cb.rayIntersection(x1, y1, z1, x2, y2, z2);

        Assertions.assertNull(intersection);
    }
}
