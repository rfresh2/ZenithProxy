package com.zenith.feature.pathfinder.goals;

import com.zenith.mc.block.BlockPos;
import com.zenith.util.math.MathHelper;
import it.unimi.dsi.fastutil.doubles.DoubleIterator;
import it.unimi.dsi.fastutil.doubles.DoubleOpenHashSet;

public record GoalNear(int x, int y, int z, int rangeSq) implements Goal, PosGoal {

    public GoalNear(BlockPos pos, int rangeSq) {
        this(pos.x(), pos.y(), pos.z(), rangeSq);
    }

    @Override
    public boolean isInGoal(int x, int y, int z) {
        return MathHelper.distanceSq3d(x, y, z, this.x, this.y, this.z) <= rangeSq;
    }

    @Override
    public double heuristic(int x, int y, int z) {
        int xDiff = x - this.x;
        int yDiff = y - this.y;
        int zDiff = z - this.z;
        return GoalBlock.calculate(xDiff, yDiff, zDiff);
    }

    @Override
    public double heuristic() {// TODO less hacky solution
        int range = (int) Math.ceil(Math.sqrt(rangeSq));
        DoubleOpenHashSet maybeAlwaysInside = new DoubleOpenHashSet(); // see pull request #1978
        double minOutside = Double.POSITIVE_INFINITY;
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    double h = heuristic(x + dx, y + dy, z + dz);
                    if (h < minOutside && isInGoal(x + dx, y + dy, z + dz)) {
                        maybeAlwaysInside.add(h);
                    } else {
                        minOutside = Math.min(minOutside, h);
                    }
                }
            }
        }
        double maxInside = Double.NEGATIVE_INFINITY;
        DoubleIterator it = maybeAlwaysInside.iterator();
        while (it.hasNext()) {
            double inside = it.nextDouble();
            if (inside < minOutside) {
                maxInside = Math.max(maxInside, inside);
            }
        }
        return maxInside;
    }

    @Override
    public BlockPos getGoalPos() {
        return new BlockPos(x, y, z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GoalNear goal = (GoalNear) o;
        return x == goal.x
                && y == goal.y
                && z == goal.z
                && rangeSq == goal.rangeSq;
    }

    @Override
    public int hashCode() {
        return (int) BlockPos.longHash(x, y, z) + rangeSq;
    }
}
