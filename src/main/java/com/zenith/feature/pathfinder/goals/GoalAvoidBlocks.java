package com.zenith.feature.pathfinder.goals;

import com.zenith.feature.player.World;
import com.zenith.mc.block.Block;
import com.zenith.mc.block.Direction;
import lombok.Data;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Data
public class GoalAvoidBlocks implements Goal {
    private final int fromX;
    private final int fromY;
    private final int fromZ;
    private final Set<Block> blocksToAvoid;

    public GoalAvoidBlocks(int fromX, int fromY, int fromZ, Block... blocksToAvoid) {
        this.fromX = fromX;
        this.fromY = fromY;
        this.fromZ = fromZ;
        this.blocksToAvoid = new HashSet<>();
        Collections.addAll(this.blocksToAvoid, blocksToAvoid);
    }

    @Override
    public boolean isInGoal(final int x, final int y, final int z) {
        if (World.isChunkLoadedBlockPos(x, z)) {
            if (this.blocksToAvoid.contains(World.getBlock(x, y, z))) {
                return false;
            }
            for (var d : Direction.VERTICALS) {
                if (this.blocksToAvoid.contains(World.getBlock(x, y + d.y(), z))) {
                    return false;
                }
            }
            // the end of a path can leave us still colliding with an adjacent block to avoid
            // as baritone says we reached the goal as soon as feet pos reach the target
            for (var d : Direction.HORIZONTALS) {
                if (this.blocksToAvoid.contains(World.getBlock(x + d.x(), y, z + d.z()))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public double heuristic(final int x, final int y, final int z) {
        double min = Double.MAX_VALUE;
        double h = GoalXZ.calculate(fromX - x, fromZ - z);
        if (h < min) {
            min = h;
        }
        min = -min;
        return min;
    }

    @Override
    public double heuristic() {
        return Double.NEGATIVE_INFINITY;
    }
}
