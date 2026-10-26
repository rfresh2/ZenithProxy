package com.zenith.feature.player.tickable.block_entity;

import com.zenith.mc.block.Block;
import com.zenith.mc.block.BlockPos;
import lombok.Getter;

public abstract class TickableBlockEntity {
    @Getter protected final BlockPos blockPos;
    @Getter private boolean removed;

    protected TickableBlockEntity(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public abstract void tick();

    /**
     * block entity should be removed when the block at its position changes
     */
    public abstract boolean isValidBlock(Block block);

    public void setRemoved() {
        this.removed = true;
    }
}
