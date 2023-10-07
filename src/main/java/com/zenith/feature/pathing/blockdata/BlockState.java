package com.zenith.feature.pathing.blockdata;

import com.zenith.feature.pathing.BlockPos;
import com.zenith.feature.pathing.CollisionBox;

import java.util.List;

/**
 * @param id palette blockstate id
 */
public record BlockState(Block block, int id, BlockPos blockPos) {
    public boolean isSolidBlock() {
        return block.boundingBox() == BlockData.BoundingBox.BLOCK;
    }

    public List<CollisionBox> getCollisionBoxes() {
        return block.getCollisionBoxesForStateId(id);
    }
}
