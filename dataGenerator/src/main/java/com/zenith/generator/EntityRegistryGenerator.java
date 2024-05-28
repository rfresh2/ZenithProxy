package com.zenith.generator;

import com.squareup.javapoet.CodeBlock;
import com.zenith.mc.entity.EntityData;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public class EntityRegistryGenerator extends RegistryGenerator<EntityData> {
    public EntityRegistryGenerator() {
        super(EntityData.class, EntityData.class.getPackage().getName(), "EntityRegistry");
    }

    @Override
    public List<EntityData> buildDataList() {
        List<EntityData> entities = new ArrayList<>();
        Registry<EntityType<?>> entityTypeRegistry = BuiltInRegistries.ENTITY_TYPE;
        entityTypeRegistry.forEach(entity -> {
            var registryKey = entityTypeRegistry.getKey(entity);
            entities.add(new EntityData(
                entityTypeRegistry.getId(entity),
                registryKey.getPath(),
                entity.getDimensions().width,
                entity.getDimensions().height,
                org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType.valueOf(
                    registryKey.getPath().toUpperCase())
            ));
        });


        return entities;
    }

    @Override
    public String dataNameMapper(final EntityData data) {
        return data.name();
    }

    @Override
    public int idMapper(final EntityData data) {
        return data.id();
    }

    @Override
    public CodeBlock dataInitializer(final EntityData data) {
        return CodeBlock.of("new $T($L, $S, $Lf, $Lf, $T.$L)",
                            EntityData.class,
                            data.id(),
                            data.name(),
                            data.width(),
                            data.height(),
                            org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType.class,
                            data.mcplType()
        );
    }
}
