package com.zenith.feature.pathfinder.calc;

import com.google.common.collect.Lists;
import com.zenith.feature.pathfinder.goals.Goal;
import com.zenith.feature.pathfinder.movement.CalculationContext;
import com.zenith.feature.pathfinder.movement.IMovement;
import com.zenith.feature.pathfinder.movement.Movement;
import com.zenith.feature.pathfinder.movement.Moves;
import com.zenith.mc.block.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.zenith.Globals.PATH_LOG;

public class Path extends PathBase {

    /**
     * The start position of this path
     */
    private final BlockPos start;

    /**
     * The end position of this path
     */
    private final BlockPos end;

    /**
     * The blocks on the path. Guaranteed that path.get(0) equals start and
     * path.get(path.size()-1) equals end
     */
    private final List<BlockPos> path;

    private final List<Movement> movements;

    private final List<PathNode> nodes;

    private final Goal goal;

    private final int numNodes;

    private final CalculationContext context;

    private volatile boolean verified;

    Path(BlockPos realStart, PathNode start, PathNode end, int numNodes, Goal goal, CalculationContext context) {
        this.end = new BlockPos(end.x(), end.y(), end.z());
        this.numNodes = numNodes;
        this.movements = new ArrayList<>();
        this.goal = goal;
        this.context = context;

        PathNode current = end;
        List<BlockPos> tempPath = new ArrayList<>();
        List<PathNode> tempNodes = new ArrayList<>();
        while (current != null) {
            tempNodes.add(current);
            tempPath.add(new BlockPos(current.x(), current.y(), current.z()));
            current = current.previous;
        }

        // If the position the player is at is different from the position we told A* to start from,
        // and A* gave us no movements, then add a fake node that will allow a movement to be created
        // that gets us to the single position in the path.
        // See PathingBehavior#createPathfinder and https://github.com/cabaletta/baritone/pull/4519
        var startNodePos = new BlockPos(start.x(), start.y(), start.z());
        if (!realStart.equals(startNodePos) && start.equals(end)) {
            this.start = realStart;
            PathNode fakeNode = new PathNode(realStart.x(), realStart.y(), realStart.z(), goal);
            fakeNode.cost = 0;
            tempNodes.add(fakeNode);
            tempPath.add(realStart);
        } else {
            this.start = startNodePos;
        }

        // Nodes are traversed last to first so we need to reverse the list
        this.path = Lists.reverse(tempPath);
        this.nodes = Lists.reverse(tempNodes);
    }

    @Override
    public Goal getGoal() {
        return goal;
    }

    private boolean assembleMovements() {
        if (path.isEmpty() || !movements.isEmpty()) {
            throw new IllegalStateException();
        }
        for (int i = 0; i < path.size() - 1; i++) {
            double cost = nodes.get(i + 1).cost - nodes.get(i).cost;
            Movement move = runBackwards(path.get(i), path.get(i + 1), cost);
            if (move == null) {
                return true;
            } else {
                movements.add(move);
            }
        }
        return false;
    }

    private Movement runBackwards(BlockPos src, BlockPos dest, double cost) {
        for (Moves moves : Moves.values()) {
            Movement move = moves.apply0(context, src);
            if (move.getDest().equals(dest)) {
                // have to calculate the cost at calculation time so we can accurately judge whether a cost increase happened between cached calculation and real execution
                // however, taking into account possible favoring that could skew the node cost, we really want the stricter limit of the two
                // so we take the minimum of the path node cost difference, and the calculated cost
                move.override(Math.min(move.calculateCost(context), cost));
                return move;
            }
        }
        // this is no longer called from bestPathSoFar, now it's in postprocessing
        PATH_LOG.debug("Movement became impossible during calculation {} {} {}", src, dest, dest.subtract(src));
        return null;
    }

    @Override
    public IPath postProcess() {
        if (verified) {
            throw new IllegalStateException();
        }
        verified = true;
        boolean failed = assembleMovements();
        movements.forEach(m -> m.checkLoadedChunk());

        if (failed) { // at least one movement became impossible during calculation
            CutoffPath res = new CutoffPath(this, movements().size());
            if (res.movements().size() != movements.size()) {
                throw new IllegalStateException();
            }
            return res;
        }
        // more post processing here
        sanityCheck();
        return this;
    }

    @Override
    public List<IMovement> movements() {
        if (!verified) {
            throw new IllegalStateException();
        }
        return Collections.unmodifiableList(movements);
    }

    @Override
    public List<BlockPos> positions() {
        return Collections.unmodifiableList(path);
    }

    @Override
    public int getNumNodesConsidered() {
        return numNodes;
    }

    @Override
    public BlockPos getSrc() {
        return start;
    }

    @Override
    public BlockPos getDest() {
        return end;
    }
}
