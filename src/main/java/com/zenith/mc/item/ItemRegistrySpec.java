package com.zenith.mc.item;

import com.zenith.mc.JsonRegistrySpec;

public class ItemRegistrySpec implements JsonRegistrySpec<ItemData> {
    @Override
    public String filePath() {
        return "/mcdata/items.json";
    }

    @Override
    public Class<ItemData> dataClass() {
        return ItemData.class;
    }
}
