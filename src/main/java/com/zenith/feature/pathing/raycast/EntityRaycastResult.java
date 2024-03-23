package com.zenith.feature.pathing.raycast;

import com.zenith.cache.data.entity.Entity;
import org.jetbrains.annotations.Nullable;

public record EntityRaycastResult(boolean hit, @Nullable Entity entity) {
    public static EntityRaycastResult miss() {
        return new EntityRaycastResult(false, null);
    }
}
