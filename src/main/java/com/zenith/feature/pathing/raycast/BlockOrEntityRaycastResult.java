package com.zenith.feature.pathing.raycast;

import org.jetbrains.annotations.Nullable;

public record BlockOrEntityRaycastResult(boolean hit, @Nullable BlockRaycastResult block, @Nullable EntityRaycastResult entity) {
    public static BlockOrEntityRaycastResult miss() {
        return new BlockOrEntityRaycastResult(false, null, null);
    }

    public boolean isEntity() {
        return entity() != null;
    }

    public boolean isBlock() {
        return block() != null;
    }
}
