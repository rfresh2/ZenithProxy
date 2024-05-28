package com.zenith.mc.block;

public record CollisionBox(double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
    public boolean intersects(final CollisionBox collisionBox) {
        return this.maxX >= collisionBox.minX && this.minX <= collisionBox.maxX
            && this.maxZ >= collisionBox.minZ && this.minZ <= collisionBox.maxZ
            && this.maxY >= collisionBox.minY && this.minY <= collisionBox.maxY;
    }
}


