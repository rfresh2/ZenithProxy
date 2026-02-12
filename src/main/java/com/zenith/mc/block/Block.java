package com.zenith.mc.block;

import com.zenith.mc.RegistryData;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

public record Block(
    int id,
    String name,
    int minStateId,
    int maxStateId,
    int mapColorId,
    BlockOffsetType offsetType,
    float maxHorizontalOffset,
    float maxVerticalOffset,
    boolean solidBlock,
    float destroySpeed,
    boolean requiresCorrectToolForDrops,
    EnumSet<BlockTags> blockTags,
    float friction,
    float speedFactor,
    float jumpFactor,
    boolean fallingBlock,
    @Nullable BlockEntityType blockEntityType
) implements RegistryData {
    @Override
    public String toString() {
        return "Block[name=" + name + ", id=" + id + "]";
    }

    public boolean isAir() {
        return blockTags().contains(BlockTags.AIR);
    }

    public boolean replaceable() {
        return blockTags().contains(BlockTags.REPLACEABLE);
    }
}
