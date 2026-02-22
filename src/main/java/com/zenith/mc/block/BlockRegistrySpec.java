package com.zenith.mc.block;

import com.zenith.mc.JsonRegistrySpec;

public class BlockRegistrySpec implements JsonRegistrySpec<Block> {
    @Override
    public String filePath() {
        return "/mcdata/blocks.smile";
    }

    @Override
    public Class<Block> dataClass() {
        return Block.class;
    }
}
