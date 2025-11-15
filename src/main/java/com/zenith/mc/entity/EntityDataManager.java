package com.zenith.mc.entity;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenith.cache.data.entity.EntityLiving;
import com.zenith.mc.block.LocalizedCollisionBox;
import com.zenith.util.struct.Maps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.SneakyThrows;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;

import static com.zenith.Globals.OBJECT_MAPPER;

public class EntityDataManager {
    private static final Reference2ObjectMap<EntityType, EntityData> entityTypeToData = new Reference2ObjectOpenHashMap<>(EntityRegistry.REGISTRY.size(), Maps.FAST_LOAD_FACTOR);
    private static final Int2ObjectMap<EntityAttachment> attachments = new Int2ObjectOpenHashMap<>();

    static {
        init();
    }

    @SneakyThrows
    private static void init() {
        for (var entry : EntityRegistry.REGISTRY.getIdMap().int2ObjectEntrySet()) {
            var entity = entry.getValue();
            entityTypeToData.put(entity.mcplType(), entity);
        }
        try (JsonParser attachmentsParse = OBJECT_MAPPER.createParser(EntityDataManager.class.getResourceAsStream("/mcdata/entityAttachments.json"))) {
            TreeNode treeNode = attachmentsParse.getCodec().readTree(attachmentsParse);
            ObjectNode attachmentsNode = (ObjectNode) treeNode;
            for (Iterator<String> it = attachmentsNode.fieldNames(); it.hasNext(); ) {
                String entityIdStr = it.next();
                int entityId = Integer.parseInt(entityIdStr);
                ArrayNode attachmentNode = (ArrayNode) attachmentsNode.get(entityIdStr);
                double passenger = attachmentNode.get(0).asDouble();
                double vehicle = attachmentNode.get(1).asDouble();
                EntityAttachment attachment = new EntityAttachment(passenger, vehicle);
                attachments.put(entityId, attachment);
            }
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

    public @Nullable EntityAttachment getAttachment(final int id) {
        return attachments.get(id);
    }
}
