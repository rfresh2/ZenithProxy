package com.zenith.feature.world.raycast;

import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.cache.data.entity.EntityStandard;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.jetbrains.annotations.Nullable;

public record EntityRaycastResult(boolean hit, @Nullable Entity entity) {
    public static EntityRaycastResult miss() {
        return new EntityRaycastResult(false, null);
    }

    public @Nullable EntityType entityType() {
        if (entity instanceof EntityStandard e) {
            return e.getEntityType();
        } else if (entity instanceof EntityPlayer p) {
            return EntityType.PLAYER;
        }
        return null;
    }
}
