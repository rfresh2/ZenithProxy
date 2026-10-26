package com.zenith.mc.block;

import com.zenith.feature.player.World;
import com.zenith.feature.player.tickable.block_entity.piston.PistonMovingBlockEntity;
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
        if (block == BlockRegistry.MOVING_PISTON) {
            var tickableBlockEntity = World.getTickableBlockEntity(x, y, z);
            if (tickableBlockEntity instanceof PistonMovingBlockEntity piston) {
                return piston.getCollisionBoxes();
            }
        }
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

    public <T extends Comparable<T>> @Nullable BlockState withPropertyValue(Property<T> property, T value) {
        var stateDefinition = BlockStatePropertyRegistry.STATES.get(block.id());
        if (stateDefinition == null || !stateDefinition.hasProperty(property)) {
            return null;
        }
        var currentValues = stateDefinition.getStates().get(id - block.minStateId());
        if (value.equals(currentValues.get(property))) {
            return this;
        }
        OUTER: for (int stateId = block.minStateId(); stateId <= block.maxStateId(); stateId++) {
            var values = stateDefinition.getStates().get(stateId - block.minStateId());
            for (var entry : values.entrySet()) {
                var expected = entry.getKey() == property
                    ? value
                    : currentValues.get(entry.getKey());
                if (!expected.equals(entry.getValue())) {
                    continue OUTER;
                }
            }
            return new BlockState(block, stateId, x, y, z);
        }
        return null;
    }

    public boolean is(Block block) {
        return this.block == block;
    }

    public boolean isAir() {
        return block.isAir();
    }

    public BlockPos pos() {
        return new BlockPos(x, y, z);
    }
}
