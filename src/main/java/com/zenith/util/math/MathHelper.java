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

    public static float clamp(final float value, final float min, final float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double[] translateEntity(double x, double y, double z, float yaw, float pitch, double distance) {
        double yawRadians = Math.toRadians(yaw);
        double pitchRadians = Math.toRadians(pitch);

        double xOffset = -Math.sin(yawRadians) * Math.cos(pitchRadians) * distance;
        double yOffset = -Math.sin(pitchRadians) * distance;
        double zOffset = Math.cos(yawRadians) * Math.cos(pitchRadians) * distance;

        double newX = x + xOffset;
        double newY = y + yOffset;
        double newZ = z + zOffset;

        return new double[]{newX,newY,newZ};
    }

}
