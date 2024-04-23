package com.zenith.generator;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zenith.DataGenerator;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.projectile.Projectile;

import java.io.FileWriter;
import java.io.Writer;

public class Entities implements Generator {
    @Override
    public void generate() {
        JsonArray resultArray = new JsonArray();
        Registry<EntityType<?>> entityTypeRegistry = BuiltInRegistries.ENTITY_TYPE;
        entityTypeRegistry.forEach(entity -> resultArray.add(generateEntity(entityTypeRegistry, entity)));
        try (Writer out = new FileWriter(DataGenerator.outputFile("entities.json"))) {
            DataGenerator.gson.toJson(resultArray, out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        DataGenerator.LOG.info("Dumped entities.json");
    }

    private JsonObject generateEntity(final Registry<EntityType<?>> registry, final EntityType<?> entityType) {
        JsonObject entityDesc = new JsonObject();
        var registryKey = registry.getKey(entityType);
        int entityRawId = registry.getId(entityType);

        entityDesc.addProperty("id", entityRawId);
        entityDesc.addProperty("name", registryKey.getPath());

        entityDesc.addProperty("displayName", Language.getInstance().getOrDefault(entityType.getDescriptionId()));
        entityDesc.addProperty("width", entityType.getDimensions().width());
        entityDesc.addProperty("height", entityType.getDimensions().height());

        String entityTypeString = "UNKNOWN";
        MinecraftServer minecraftServer = DataGenerator.SERVER_INSTANCE;

        if (minecraftServer != null) {
            var entityObject = entityType.create(minecraftServer.overworld());
            entityTypeString = entityObject != null ? getEntityTypeForClass(entityObject.getClass()) : "player";
        }
        entityDesc.addProperty("type", entityTypeString);

        return entityDesc;
    }

    //Honestly, both "type" and "category" fields in the schema and examples do not contain any useful information
    //Since category is optional, I will just leave it out, and for type I will assume general entity classification
    //by the Entity class hierarchy (which has some weirdness too by the way)
    private static String getEntityTypeForClass(Class<? extends Entity> entityClass) {
        //Top-level classifications
        if (WaterAnimal.class.isAssignableFrom(entityClass)) {
            return "water_creature";
        }
        if (Animal.class.isAssignableFrom(entityClass)) {
            return "animal";
        }
        if (Monster.class.isAssignableFrom(entityClass)) {
            return "hostile";
        }
        if (AmbientCreature.class.isAssignableFrom(entityClass)) {
            return "ambient";
        }

        //Second level classifications. PathAwareEntity is not included because it
        //doesn't really make much sense to categorize by it
        if (Npc.class.isAssignableFrom(entityClass)) {
            return "passive";
        }
        if (Mob.class.isAssignableFrom(entityClass)) {
            return "mob";
        }

        //Other classifications only include living entities and projectiles. everything else is categorized as other
        if (LivingEntity.class.isAssignableFrom(entityClass)) {
            return "living";
        }
        if (Projectile.class.isAssignableFrom(entityClass)) {
            return "projectile";
        }
        return "other";
    }
}
