package com.zenith.mc.entity;

import com.zenith.cache.data.entity.EntityLiving;
import com.zenith.mc.block.LocalizedCollisionBox;
import com.zenith.util.struct.Maps;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.SneakyThrows;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.jspecify.annotations.Nullable;

public class EntityDataManager {
    private static final Reference2ObjectMap<EntityType, EntityData> entityTypeToData = new Reference2ObjectOpenHashMap<>(EntityRegistry.REGISTRY.size(), Maps.FAST_LOAD_FACTOR);

    static {
        init();
    }

    @SneakyThrows
    private static void init() {
        for (var entry : EntityRegistry.REGISTRY.getIdMap().int2ObjectEntrySet()) {
            var entity = entry.getValue();
            entityTypeToData.put(entity.mcplType(), entity);
        }
    }

    /**
     * @deprecated Use {@link EntityRegistry#REGISTRY} instead.
     */
    @Deprecated
    public EntityData getEntityData(final int id) {
        return EntityRegistry.REGISTRY.get(id);
    }

    public EntityData getEntityData(final EntityType type) {
        var data = entityTypeToData.get(type);
        if (data == entityTypeToData.defaultReturnValue()) return null;
        return data;
    }

    public LocalizedCollisionBox getCollisionBox(final EntityLiving entity) {
        var data = getEntityData(entity.getEntityType());
        if (data == null) return null;
        var dimensions = entity.dimensions();
        double w = dimensions.getX() / 2;
        return new LocalizedCollisionBox(
            entity.getX() - w, entity.getX() + w,
            entity.getY(), entity.getY() + dimensions.getY(),
            entity.getZ() - w, entity.getZ() + w,
            entity.getX(), entity.getY(), entity.getZ()
        );
    }

    /**
     * @deprecated Use {@link EntityData#entityAttachment()} instead.
     */
    @Deprecated
    public @Nullable EntityAttachment getAttachment(final int id) {
        return EntityRegistry.REGISTRY.get(id).entityAttachment();
    }
}
