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

    public static double wrapDegrees(double degrees) {
        double d = degrees % 360.0;
        if (d >= 180.0) d -= 360.0;
        if (d < -180.0) d += 360.0;
        return d;
    }

    public static float wrapDegrees(float degrees) {
        float f = degrees % 360.0F;
        if (f >= 180.0F) f -= 360.0F;
        if (f < -180.0F) f += 360.0F;
        return f;
    }
}
