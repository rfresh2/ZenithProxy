package com.zenith.mc.block;

import java.util.ArrayList;
import java.util.List;

import static com.zenith.Shared.BLOCK_DATA;

/**
 * @param id palette blockstate id
 */
public record BlockState(Block block, int id, int x, int y, int z) {
    public boolean isSolidBlock() {
        return block.isBlock();
    }

    public List<CollisionBox> getCollisionBoxes() {
        return BLOCK_DATA.getCollisionBoxesFromBlockStateId(id);
    }

    public List<LocalizedCollisionBox> getLocalizedCollisionBoxes() {
        var collisionBoxes = getCollisionBoxes();
        var offsetVec = block.offsetType().getOffsetFunction().offset(this, x, y, z);
        final List<LocalizedCollisionBox> localizedCollisionBoxes = new ArrayList<>(collisionBoxes.size());
        for (int i = 0; i < collisionBoxes.size(); i++) {
            var collisionBox = collisionBoxes.get(i);
            localizedCollisionBoxes.add(new LocalizedCollisionBox(
                collisionBox.minX() + offsetVec.getX() + x,
                collisionBox.maxX() + offsetVec.getX() + x,
                collisionBox.minY() + offsetVec.getY() + y,
                collisionBox.maxY() + offsetVec.getY() + y,
                collisionBox.minZ() + offsetVec.getZ() + z,
                collisionBox.maxZ() + offsetVec.getZ() + z,
                x, y, z
            ));
        }
        return localizedCollisionBoxes;
    }
}
