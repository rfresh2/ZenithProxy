package com.zenith.mc.block;

import java.util.ArrayList;
import java.util.List;

import static com.zenith.Shared.BLOCK_DATA;

/**
 * @param id palette blockstate id
 */
public record BlockState(Block block, int id) {
    public boolean isSolidBlock() {
        return block.isBlock();
    }

    public List<CollisionBox> getCollisionBoxes() {
        return BLOCK_DATA.getCollisionBoxesFromBlockStateId(id);
    }

    public List<LocalizedCollisionBox> getLocalizedCollisionBoxes(int x, int y, int z) {
        var collisionBoxes = getCollisionBoxes();
        var offsetVec = block.offsetType().getOffsetFunction().offset(this, x, y, z);
        final List<LocalizedCollisionBox> localizedCollisionBoxes = new ArrayList<>(collisionBoxes.size());
        for (int i = 0; i < collisionBoxes.size(); i++) {
            var collisionBox = collisionBoxes.get(i);
            localizedCollisionBoxes.add(new LocalizedCollisionBox(
                collisionBox.minX() + offsetVec.getX(),
                collisionBox.maxX() + offsetVec.getX(),
                collisionBox.minY() + offsetVec.getY(),
                collisionBox.maxY() + offsetVec.getY(),
                collisionBox.minZ() + offsetVec.getZ(),
                collisionBox.maxZ() + offsetVec.getZ(),
                x, y, z
            ));
        }
        return localizedCollisionBoxes;
    }
}
