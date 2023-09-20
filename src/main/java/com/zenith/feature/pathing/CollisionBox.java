package com.zenith.feature.pathing;

import lombok.Data;

@Data
public class CollisionBox {
    private final double minX;
    private final double maxX;
    private final double minY;
    private final double maxY;
    private final double minZ;
    private final double maxZ;

    public boolean intersects(final CollisionBox collisionBox) {
        return this.maxX >= collisionBox.minX && this.minX <= collisionBox.maxX
                && this.maxZ >= collisionBox.minZ && this.minZ <= collisionBox.maxZ
                && this.maxY >= collisionBox.minY && this.minY <= collisionBox.maxY;
    }
}


