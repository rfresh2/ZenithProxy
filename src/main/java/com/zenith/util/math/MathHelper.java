package com.zenith.util.math;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.math.vector.Vector3d;

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

    public static double lerp(double delta, double start, double end) {
        return start + delta * (end - start);
    }

    public static long lfloor(double d) {
        long i = (long)d;
        return d < (double)i ? i - 1L : i;
    }

    public static double frac(double d) {
        return d - (double)lfloor(d);
    }

    public static int sign(double d) {
        if (d == 0.0) {
            return 0;
        } else {
            return d > 0.0 ? 1 : -1;
        }
    }

    public static Vector3d calculateViewVector(final double yaw, final double pitch) {
        double pitchRad = pitch * (Math.PI / 180.0);
        double yawRad = -yaw * (Math.PI / 180.0);
        double yawCos = Math.cos(yawRad);
        double yawSin = Math.sin(yawRad);
        double pitchCos = Math.cos(pitchRad);
        double pitchSin = Math.sin(pitchRad);
        return Vector3d.from(yawSin * pitchCos, -pitchSin, yawCos * pitchCos);
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

    public static String formatDurationLong(Duration duration) {
        var durationInSeconds = duration.toSeconds();
        var secondsInMinute = 60L;
        var secondsInHour = secondsInMinute * 60L;
        var secondsInDay = secondsInHour * 24L;
        var secondsInMonth = secondsInDay * 30L; // assuming 30 days per month

        var months = durationInSeconds / secondsInMonth;
        var days = (durationInSeconds % secondsInMonth) / secondsInDay;
        var hours = (durationInSeconds % secondsInDay) / secondsInHour;
        final StringBuilder sb = new StringBuilder();
        sb.append((months > 0) ? months + " month" + (months != 1 ? "s" : "") + ", " : "");
        sb.append((days > 0) ? days + " day" + (days != 1 ? "s" : "") + ", " : "");
        sb.append(hours + " hour" + (hours != 1 ? "s" : ""));
        return sb.toString();
    }
}
