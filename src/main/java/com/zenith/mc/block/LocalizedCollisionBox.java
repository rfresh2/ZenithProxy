package com.zenith.mc.block;


import lombok.AllArgsConstructor;
import lombok.Data;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.jetbrains.annotations.Nullable;

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

    public double collideX(final LocalizedCollisionBox other, double x) {
        if (noYIntersection(other) || noZIntersection(other)) return x;
        return collidePushOut(this.getMinX(), this.getMaxX(), other.getMinX(), other.getMaxX(), x);
    }

    public double collideY(final LocalizedCollisionBox other, double y) {
        if (noXIntersection(other) || noZIntersection(other)) return y;
        return collidePushOut(this.getMinY(), this.getMaxY(), other.getMinY(), other.getMaxY(), y);
    }

    public double collideZ(final LocalizedCollisionBox other, double z) {
        if (noXIntersection(other) || noYIntersection(other)) return z;
        return collidePushOut(this.getMinZ(), this.getMaxZ(), other.getMinZ(), other.getMaxZ(), z);
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

    public boolean noXIntersection(final LocalizedCollisionBox other) {
        return other.getMaxX() <= this.getMinX() || other.getMinX() >= this.getMaxX();
    }

    public boolean noYIntersection(final LocalizedCollisionBox other) {
        return other.getMaxY() <= this.getMinY() || other.getMinY() >= this.getMaxY();
    }

    public boolean noZIntersection(final LocalizedCollisionBox other) {
        return other.getMaxZ() <= this.getMinZ() || other.getMinZ() >= this.getMaxZ();
    }

    public boolean intersects(final LocalizedCollisionBox other) {
        return this.maxX >= other.minX && this.minX <= other.maxX
            && this.maxZ >= other.minZ && this.minZ <= other.maxZ
            && this.maxY >= other.minY && this.minY <= other.maxY;
    }

    public boolean intersects(double oMinX, double oMaxX, double oMinY, double oMaxY, double oMinZ, double oMaxZ) {
        return this.maxX >= oMinX && this.minX <= oMaxX
            && this.maxZ >= oMinZ && this.minZ <= oMaxZ
            && this.maxY >= oMinY && this.minY <= oMaxY;

    }

    public @Nullable RayIntersection rayIntersection(
        final double x1, final double y1, final double z1, // start pos
        final double x2, final double y2, final double z2 // end pos
    ) {
        // Check if the ray's start and end points are both outside the bounding box in the same direction
        if ((x1 < minX && x2 < minX) || (x1 > maxX && x2 > maxX) ||
            (y1 < minY && y2 < minY) || (y1 > maxY && y2 > maxY) ||
            (z1 < minZ && z2 < minZ) || (z1 > maxZ && z2 > maxZ)) {
            return null;
        }
        final double xLen = x2 - x1;
        final double yLen = y2 - y1;
        final double zLen = z2 - z1;
        final double t1 = (this.minX - x1) / xLen;
        final double t2 = (this.maxX - x1) / xLen;
        final double t3 = (this.minY - y1) / yLen;
        final double t4 = (this.maxY - y1) / yLen;
        final double t5 = (this.minZ - z1) / zLen;
        final double t6 = (this.maxZ - z1) / zLen;
        final double tmin = Math.max(Math.max(Math.min(t1, t2), Math.min(t3, t4)), Math.min(t5, t6));
        final double tmax = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6));

        if (tmax < 0 || tmin > tmax) return null;

        Direction intersectingFace;
        if (tmin == t1) {
            intersectingFace = Direction.WEST;
        } else if (tmin == t2) {
            intersectingFace = Direction.EAST;
        } else if (tmin == t3) {
            intersectingFace = Direction.DOWN;
        } else if (tmin == t4) {
            intersectingFace = Direction.UP;
        } else if (tmin == t5) {
            intersectingFace = Direction.NORTH;
        } else {
            intersectingFace = Direction.SOUTH;
        }

        return new RayIntersection(x1 + tmin * xLen, y1 + tmin * yLen, z1 + tmin * zLen, intersectingFace);
    }

    public record RayIntersection(double x, double y, double z, Direction intersectingFace) { }
}
