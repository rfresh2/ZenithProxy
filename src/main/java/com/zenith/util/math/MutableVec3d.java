package com.zenith.util.math;

import lombok.*;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class MutableVec3d {
    private double x;
    private double y;
    private double z;

    public static MutableVec3d ZERO = new MutableVec3d(0, 0, 0);

    public MutableVec3d(final MutableVec3d velocity) {
        this.x = velocity.x;
        this.y = velocity.y;
        this.z = velocity.z;
    }

    public void set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void multiply(double value) {
        this.x *= value;
        this.y *= value;
        this.z *= value;
    }

    public void add(final MutableVec3d vec3d) {
        this.x += vec3d.x;
        this.y += vec3d.y;
        this.z += vec3d.z;
    }

    public void add(double x, double y, double z) {
        this.x += x;
        this.y += y;
        this.z += z;
    }

    public void multiply(final double x, final double y, final double z) {
        this.x *= x;
        this.y *= y;
        this.z *= z;
    }

    public double lengthSquared() {
        return this.x * this.x + this.y * this.y + this.z * this.z;
    }

    public void normalize() {
        double d = Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
        if (d < 1.0E-4) {
            this.x = 0.0;
            this.y = 0.0;
            this.z = 0.0;
        } else {
            this.x /= d;
            this.y /= d;
            this.z /= d;
        }
    }

    public double horizontalLengthSquared() {
        return this.x * this.x + this.z * this.z;
    }

    public double distance2d(double x, double z) {
        double d = x - this.x;
        double e = z - this.z;
        return Math.sqrt(d * d + e * e);
    }
}
