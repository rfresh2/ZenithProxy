package com.zenith.feature.world.blockdata;

import com.zenith.feature.world.BlockPos;
import com.zenith.feature.world.CollisionBox;

import java.util.List;

import static com.zenith.Shared.BLOCK_DATA;

/**
 * @param id palette blockstate id
 */
public record BlockState(Block block, int id, BlockPos blockPos) {
    public boolean isSolidBlock() {
        return block.isBlock();
    }

    public List<CollisionBox> getCollisionBoxes() {
        return BLOCK_DATA.getCollisionBoxesFromBlockStateId(id);
    }
}
