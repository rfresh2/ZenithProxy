package com.zenith.feature.pathing;


import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * CollisionBox localized to coordinates
 */
@AllArgsConstructor
@Data
public class LocalizedCollisionBox {
    private final double minX;
    private final double maxX;
    private final double minY;
    private final double maxY;
    private final double minZ;
    private final double maxZ;
    private final double x;
    private final double y;
    private final double z;

    public LocalizedCollisionBox(CollisionBox cb,
                                 final double x,
                                 final double y,
                                 final double z) {
        this.minX = cb.minX() + x;
        this.maxX = cb.maxX() + x;
        this.minY = cb.minY() + y;
        this.maxY = cb.maxY() + y;
        this.minZ = cb.minZ() + z;
        this.maxZ = cb.maxZ() + z;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public LocalizedCollisionBox stretch(double x, double y, double z) {
        return new LocalizedCollisionBox(x < 0.0 ? getMinX() + x : getMinX(),
                                         x > 0.0 ? getMaxX() + x : getMaxX(),
                                         y < 0.0 ? getMinY() + y : getMinY(),
                                         y > 0.0 ? getMaxY() + y : getMaxY(),
                                         z < 0.0 ? getMinZ() + z : getMinZ(),
                                         z > 0.0 ? getMaxZ() + z : getMaxZ(),
                                         this.x,
                                         this.y,
                                         this.z);
    }

    public LocalizedCollisionBox move(final double x, final double y, final double z) {
        return new LocalizedCollisionBox(this.getMinX() + x,
                                         this.getMaxX() + x,
                                         this.getMinY() + y,
                                         this.getMaxY() + y,
                                         this.getMinZ() + z,
                                         this.getMaxZ() + z,
                                         this.x + x,
                                         this.y + y,
                                         this.z + z);
    }

    public double collideX(final LocalizedCollisionBox otherBoundingBox, double x) {
        if (noYIntersection(otherBoundingBox) || noZIntersection(otherBoundingBox)) return x;
        return collidePushOut(this.getMinX(), this.getMaxX(), otherBoundingBox.getMinX(), otherBoundingBox.getMaxX(), x);
    }

    public double collideY(final LocalizedCollisionBox otherBoundingBox, double y) {
        if (noXIntersection(otherBoundingBox) || noZIntersection(otherBoundingBox)) return y;
        return collidePushOut(this.getMinY(), this.getMaxY(), otherBoundingBox.getMinY(), otherBoundingBox.getMaxY(), y);
    }

    public double collideZ(final LocalizedCollisionBox otherBoundingBox, double z) {
        if (noXIntersection(otherBoundingBox) || noYIntersection(otherBoundingBox)) return z;
        return collidePushOut(this.getMinZ(), this.getMaxZ(), otherBoundingBox.getMinZ(), otherBoundingBox.getMaxZ(), z);
    }

    public static double collidePushOut(double box1Min, double box1Max, double box2Min, double box2Max, double speed) {
        if (speed > 0.0F && box2Max <= box1Min) {
            double collideMax = box1Min - box2Max;
            if (collideMax < speed)
                speed = collideMax;
        }
        if (speed < 0.0F && box2Min >= box1Max) {
            double collideMax = box1Max - box2Min;
            if (collideMax > speed)
                speed = collideMax;
        }
        return speed;
    }

    public boolean noXIntersection(final LocalizedCollisionBox otherBoundingBox) {
        return otherBoundingBox.getMaxX() <= this.getMinX() || otherBoundingBox.getMinX() >= this.getMaxX();
    }

    public boolean noYIntersection(final LocalizedCollisionBox otherBoundingBox) {
        return otherBoundingBox.getMaxY() <= this.getMinY() || otherBoundingBox.getMinY() >= this.getMaxY();
    }

    public boolean noZIntersection(final LocalizedCollisionBox otherBoundingBox) {
        return otherBoundingBox.getMaxZ() <= this.getMinZ() || otherBoundingBox.getMinZ() >= this.getMaxZ();
    }

    public boolean intersects(final LocalizedCollisionBox other) {
        return this.maxX >= other.minX && this.minX <= other.maxX
            && this.maxZ >= other.minZ && this.minZ <= other.maxZ
            && this.maxY >= other.minY && this.minY <= other.maxY;
    }
}
