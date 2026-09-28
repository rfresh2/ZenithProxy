package com.zenith.feature.pathfinder.calc;

import com.zenith.feature.pathfinder.goals.Goal;
import com.zenith.feature.pathfinder.movement.ActionCosts;
import com.zenith.mc.block.BlockPos;

public final class PathNode {
    /**
     * The position of this node
     */
    public long pos;

    /**
     * Cached, should always be equal to goal.heuristic(pos)
     */
    public final float estimatedCostToGoal;

    /**
     * Total cost of getting from start to here
     * Mutable and changed by PathFinder
     */
    public float cost;

    /**
     * Should always be equal to estimatedCosttoGoal + cost
     * Mutable and changed by PathFinder
     */
    public float combinedCost() {
        return cost + estimatedCostToGoal;
    }

    /**
     * In the graph search, what previous node contributed to the cost
     * Mutable and changed by PathFinder
     */
    public PathNode previous;

    /**
     * Where is this node in the array flattenization of the binary heap? Needed for decrease-key operations.
     */
    public int heapPosition;

    public PathNode(int x, int y, int z, Goal goal) {
        this.previous = null;
        this.cost = (float) ActionCosts.COST_INF;
        this.estimatedCostToGoal = (float) goal.heuristic(x, y, z);
        if (Float.isNaN(estimatedCostToGoal)) {
            throw new IllegalStateException(goal + " calculated implausible heuristic");
        }
        this.heapPosition = -1;
        this.pos = BlockPos.asLong(x, y, z);
    }

    public boolean isOpen() {
        return heapPosition != -1;
    }

    public int x() {
        return BlockPos.getX(pos);
    }

    public int y() {
        return BlockPos.getY(pos);
    }

    public int z() {
        return BlockPos.getZ(pos);
    }

    /**
     * TODO: Possibly reimplement hashCode and equals. They are necessary for this class to function but they could be done better
     *
     * @return The hash code value for this {@link PathNode}
     */
    @Override
    public int hashCode() {
        return (int) BlockPos.longHash(x(), y(), z());
    }

    @Override
    public boolean equals(Object obj) {
        // GOTTA GO FAST
        // ALL THESE CHECKS ARE FOR PEOPLE WHO WANT SLOW CODE
        // SKRT SKRT
        //if (obj == null || !(obj instanceof PathNode)) {
        //    return false;
        //}

        final PathNode other = (PathNode) obj;
        //return Objects.equals(this.pos, other.pos) && Objects.equals(this.goal, other.goal);

        return pos == other.pos;
    }
}
