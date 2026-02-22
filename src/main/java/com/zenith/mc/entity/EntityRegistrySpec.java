package com.zenith.mc.entity;

import com.zenith.mc.JsonRegistrySpec;

public class EntityRegistrySpec implements JsonRegistrySpec<EntityData> {
    @Override
    public String filePath() {
        return "/mcdata/entities.smile";
    }

    @Override
    public Class<EntityData> dataClass() {
        return EntityData.class;
    }
}
