package com.zenith.event.module;

import com.github.steveice10.mc.protocol.data.game.entity.object.ProjectileData;
import com.zenith.cache.data.entity.EntityStandard;

public record EntityFishHookSpawnEvent(EntityStandard fishHookObject) {

    public ProjectileData getProjectileData() {
        return (ProjectileData) fishHookObject().getObjectData();
    }

    public int getOwnerEntityId() {
        return getProjectileData().getOwnerId();
    }
}
