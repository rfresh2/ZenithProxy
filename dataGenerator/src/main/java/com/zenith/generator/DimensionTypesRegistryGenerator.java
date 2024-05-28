package com.zenith.generator;

import com.squareup.javapoet.CodeBlock;
import com.zenith.DataGenerator;
import com.zenith.feature.world.dimension.DimensionData;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.ArrayList;
import java.util.List;

public class DimensionTypesRegistryGenerator extends RegistryGenerator<DimensionData> {
    public DimensionTypesRegistryGenerator() {
        super(DimensionData.class, DimensionData.class.getPackage().getName(), "DimensionRegistry");
    }

    @Override
    public List<DimensionData> buildDataList() {
        List<DimensionData> result = new ArrayList<>();
        Registry<DimensionType> registry = DataGenerator.SERVER_INSTANCE.registryAccess()
            .registry(Registries.DIMENSION_TYPE)
            .get();
        IdMap<Holder<DimensionType>> holderIdMap = registry.asHolderIdMap();
        for (int id = 0; id < holderIdMap.size(); id++) {
            Holder<DimensionType> holder = holderIdMap.byId(id);
            var dimension = holder.value();
            result.add(new DimensionData(
                id,
                holder.unwrapKey().get().location().getPath(),
                dimension.minY(),
                dimension.minY() + dimension.height(),
                dimension.height()
            ));
        }
        return result;
    }

    @Override
    public String dataNameMapper(final DimensionData data) {
        return data.name();
    }

    @Override
    public int idMapper(final DimensionData data) {
        return data.id();
    }

    @Override
    public CodeBlock dataInitializer(final DimensionData data) {
        return CodeBlock.of("new $T($L, $S, $L, $L, $L)",
                            this.dataType,
                            data.id(),
                            data.name(),
                            data.minY(),
                            data.buildHeight(),
                            data.height());
    }
}
