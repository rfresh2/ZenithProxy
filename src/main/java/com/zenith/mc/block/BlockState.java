package com.zenith.mc.block;

import com.zenith.feature.player.World;
import com.zenith.mc.block.properties.api.BlockStateProperties;
import com.zenith.mc.block.properties.api.Property;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

import static com.zenith.Globals.BLOCK_DATA;

public record BlockState(Block block, int id, int x, int y, int z) {
    public boolean isSolidBlock() {
        return block.solidBlock();
    }

    public boolean isShapeFullBlock() {
        List<CollisionBox> collisionBoxes = getCollisionBoxes();
        if (collisionBoxes.size() != 1) {
            return false;
        }
        var cb = collisionBoxes.getFirst();
        return cb.isFullBlock();
    }

    public List<CollisionBox> getCollisionBoxes() {
        return BLOCK_DATA.getCollisionBoxesFromBlockStateId(id);
    }

    public List<CollisionBox> getInteractionBoxes() {
        return BLOCK_DATA.getInteractionBoxesFromBlockStateId(id);
    }

    public List<LocalizedCollisionBox> getLocalizedCollisionBoxes() {
        var collisionBoxes = getCollisionBoxes();
        return BLOCK_DATA.localizeCollisionBoxes(collisionBoxes, block, x, y, z);
    }

    public List<LocalizedCollisionBox> getLocalizedInteractionBoxes() {
        var collisionBoxes = getInteractionBoxes();
        return BLOCK_DATA.localizeCollisionBoxes(collisionBoxes, block, x, y, z);
    }

    public boolean isPathfindable() {
        return BLOCK_DATA.isPathfindable(id);
    }

    /** Available properties: {@link BlockStateProperties} **/
    public @Nullable <T extends Comparable<T>> T getProperty(Property<T> property) {
        return World.getBlockStateProperty(block, id, property);
    }

    /** Available properties: {@link BlockStateProperties} **/
    public boolean hasProperty(Property<?> property) {
        return World.hasBlockStateProperty(block, property);
    }

    public Collection<Property<?>> getProperties() {
        return World.getBlockStateProperties(block);
    }
}
