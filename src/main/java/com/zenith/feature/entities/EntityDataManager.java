package com.zenith.feature.entities;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

import java.util.Iterator;

import static com.zenith.Shared.OBJECT_MAPPER;

public class EntityDataManager {
    private final Object2ObjectMap<String, EntityData> entityNameToData = new Object2ObjectOpenHashMap<>(130);
    private final Reference2ObjectMap<EntityType, EntityData> entityTypeToData = new Reference2ObjectOpenHashMap<>(130);

    public EntityDataManager() {
        init();
    }

    private void init() {
        try (JsonParser parser = OBJECT_MAPPER.createParser(getClass().getResourceAsStream("/mcdata/entities.json"))) {
            TreeNode node = parser.getCodec().readTree(parser);
            for (Iterator<JsonNode> it = ((ArrayNode) node).elements(); it.hasNext(); ) {
                final var e = it.next();
                String name = e.get("name").asText();
                double width = e.get("width").asDouble();
                double height = e.get("height").asDouble();
                EntityType type = EntityType.valueOf(name.toUpperCase());
                var data = new EntityData(name, width, height, type);
                entityNameToData.put(name, data);
                entityTypeToData.put(type, data);
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public EntityData getEntityData(final String name) {
        var data = entityNameToData.get(name);
        if (data == entityNameToData.defaultReturnValue()) return null;
        return data;
    }

    public EntityData getEntityData(final EntityType type) {
        var data = entityTypeToData.get(type);
        if (data == entityTypeToData.defaultReturnValue()) return null;
        return data;
    }
}
