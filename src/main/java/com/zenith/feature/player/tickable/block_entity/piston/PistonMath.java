package com.zenith.feature.player.tickable.block_entity.piston;

import com.zenith.mc.block.Direction;
import com.zenith.mc.block.LocalizedCollisionBox;

public final class PistonMath {
    private PistonMath() {}

    public static LocalizedCollisionBox getMovementArea(LocalizedCollisionBox bounds, Direction dir, double delta) {
        double d0 = delta * dir.getAxisDirection().getStep();
        double d1 = Math.min(d0, 0.0);
        double d2 = Math.max(d0, 0.0);
        return switch (dir) {
            case WEST -> new LocalizedCollisionBox(bounds.minX() + d1, bounds.minX() + d2, bounds.minY(), bounds.maxY(), bounds.minZ(), bounds.maxZ(), bounds.x(), bounds.y(), bounds.z());
            case EAST -> new LocalizedCollisionBox(bounds.maxX() + d1, bounds.maxX() + d2, bounds.minY(), bounds.maxY(), bounds.minZ(), bounds.maxZ(), bounds.x(), bounds.y(), bounds.z());
            case DOWN -> new LocalizedCollisionBox(bounds.minX(), bounds.maxX(), bounds.minY() + d1, bounds.minY() + d2, bounds.minZ(), bounds.maxZ(), bounds.x(), bounds.y(), bounds.z());
            case UP -> new LocalizedCollisionBox(bounds.minX(), bounds.maxX(), bounds.maxY() + d1, bounds.maxY() + d2, bounds.minZ(), bounds.maxZ(), bounds.x(), bounds.y(), bounds.z());
            case NORTH -> new LocalizedCollisionBox(bounds.minX(), bounds.maxX(), bounds.minY(), bounds.maxY(), bounds.minZ() + d1, bounds.minZ() + d2, bounds.x(), bounds.y(), bounds.z());
            case SOUTH -> new LocalizedCollisionBox(bounds.minX(), bounds.maxX(), bounds.minY(), bounds.maxY(), bounds.maxZ() + d1, bounds.maxZ() + d2, bounds.x(), bounds.y(), bounds.z());
        };
    }
}
