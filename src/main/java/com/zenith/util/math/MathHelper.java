package com.zenith.util.math;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MathHelper {
    public static double squaredMagnitude(double a, double b, double c) {
        return a * a + b * b + c * c;
    }

    public static double square(double n) {
        return n * n;
    }

    public static double round(double n, int places) {
        double scale = Math.pow(10, places);
        return Math.round(n * scale) / scale;
    }

    public static int floorToInt(final double d) {
        final int i = (int)d;
        return d < i ? i - 1 : i;
    }

    public static int ceilToInt(final double d) {
        final int i = (int)d;
        return d > i ? i + 1 : i;
    }
}
