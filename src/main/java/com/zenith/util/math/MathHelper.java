package com.zenith.util.math;

import lombok.experimental.UtilityClass;

import java.time.Duration;

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

    public static int log2Ceil(int num) {
        return (int) Math.ceil(Math.log(num) / Math.log(2));
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

    public static float wrapPitch(float pitch) {
        float f = pitch % 180.0F;
        if (f >= 90.0F) f -= 180.0F;
        if (f < -90.0F) f += 180.0F;
        return f;
    }

    public static float wrapYaw(float yaw) {
        return wrapDegrees(yaw);
    }

    public static float clamp(final float value, final float min, final float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double distance2d(double x1, double y1, double x2, double y2) {
        return Math.sqrt(square(x1 - x2) + square(y1 - y2));
    }

    public static double manhattanDistance2d(double x1, double y1, double x2, double y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    // is this math? no, but idk where else to put it
    public static String formatDuration(Duration duration) {
        final StringBuilder sb = new StringBuilder();
        if (duration.toDaysPart() > 0) sb.append(duration.toDaysPart()).append("d ");
        if (duration.toHoursPart() > 0) sb.append(duration.toHoursPart()).append("h ");
        if (duration.toMinutesPart() > 0) sb.append(duration.toMinutesPart()).append("m ");
        if (duration.toSecondsPart() > 0 || sb.isEmpty()) sb.append(duration.toSecondsPart()).append("s");
        return sb.toString().trim();
    }
}
