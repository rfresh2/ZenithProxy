package com.zenith.feature.pathfinder.goals;

import com.zenith.feature.player.World;
import com.zenith.mc.block.Block;
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
            var p1 = World.getBlock(x, y, z);
            var p2 = World.getBlock(x, y-1, z);
            return !this.blocksToAvoid.contains(p1) && !this.blocksToAvoid.contains(p2);
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
