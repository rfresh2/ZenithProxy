package com.zenith.feature.pathfinder.process.elytra.pathfinder;

/** The edge length of a cube in the octree, as a power of two. */
enum Size {
    X1, X2, X4, X8, X16;

    private static final Size[] VALUES = values();

    int shift() {
        return ordinal();
    }

    int width() {
        return 1 << ordinal();
    }

    Size smaller() {
        return VALUES[ordinal() - 1];
    }

    Size larger() {
        return VALUES[ordinal() + 1];
    }
}
