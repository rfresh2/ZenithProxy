package com.zenith.generator;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zenith.DataGenerator;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.DimensionType;

import java.io.FileWriter;
import java.io.Writer;

public class DimensionTypes implements Generator {
    @Override
    public void generate() {
        JsonArray dimensions = new JsonArray();
        Registry<DimensionType> registry = DataGenerator.SERVER_INSTANCE.registryAccess()
            .registry(Registries.DIMENSION_TYPE)
            .get();
        IdMap<Holder<DimensionType>> holderIdMap = registry.asHolderIdMap();
        for (int id = 0; id < holderIdMap.size(); id++) {
            Holder<DimensionType> holder = holderIdMap.byId(id);
            dimensions.add(generateDimensionData(id, holder.unwrapKey().get(), holder.value()));
        }

        try (Writer out = new FileWriter(DataGenerator.outputFile("dimensions.json"))) {
            DataGenerator.gson.toJson(dimensions, out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        DataGenerator.LOG.info("Dumped dimensions.json");
    }

    public JsonObject generateDimensionData(int id, ResourceKey<DimensionType> key, DimensionType dimensionType) {
        JsonObject dimensionData = new JsonObject();
        dimensionData.addProperty("id", id);
        dimensionData.addProperty("name", key.location().getPath());
        dimensionData.addProperty("coordinateScale", dimensionType.coordinateScale());
        dimensionData.addProperty("hasCeiling", dimensionType.hasCeiling());
        dimensionData.addProperty("hasSkyLight", dimensionType.hasSkyLight());
        dimensionData.addProperty("ultraWarm", dimensionType.ultraWarm());
        dimensionData.addProperty("natural", dimensionType.natural());
        dimensionData.addProperty("piglinSafe", dimensionType.piglinSafe());
        dimensionData.addProperty("bedWorks", dimensionType.bedWorks());
        dimensionData.addProperty("respawnAnchorWorks", dimensionType.respawnAnchorWorks());
        dimensionData.addProperty("hasRaids", dimensionType.hasRaids());
        dimensionData.addProperty("logicalHeight", dimensionType.logicalHeight());
        return dimensionData;
    }
}
