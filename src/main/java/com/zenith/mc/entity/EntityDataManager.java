package com.zenith.mc.entity;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

public class EntityDataManager {
    private final Reference2ObjectMap<EntityType, EntityData> entityTypeToData = new Reference2ObjectOpenHashMap<>(EntityRegistry.REGISTRY.size());

    public EntityDataManager() {
        init();
    }

    private void init() {
        for (var entry : EntityRegistry.REGISTRY.getIdMap().int2ObjectEntrySet()) {
            var entity = entry.getValue();
            entityTypeToData.put(entity.mcplType(), entity);
        }
    }

    public EntityData getEntityData(final int id) {
        return EntityRegistry.REGISTRY.get(id);
    }

    public EntityData getEntityData(final EntityType type) {
        var data = entityTypeToData.get(type);
        if (data == entityTypeToData.defaultReturnValue()) return null;
        return data;
    }
}
