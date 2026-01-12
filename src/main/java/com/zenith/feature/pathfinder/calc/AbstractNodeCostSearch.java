package com.zenith.feature.pathfinder.calc;

import com.zenith.feature.pathfinder.goals.Goal;
import com.zenith.feature.pathfinder.movement.CalculationContext;
import com.zenith.feature.pathfinder.util.PathCalculationResult;
import com.zenith.mc.block.BlockPos;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.Optional;

import static com.zenith.Globals.PATH_LOG;

public abstract class AbstractNodeCostSearch {
    protected final BlockPos realStart;
    protected final int startX;
    protected final int startY;
    protected final int startZ;

    protected final Goal goal;

    private final CalculationContext context;

    /**
     * @see <a href="https://github.com/cabaletta/baritone/issues/107">Issue #107</a>
     */
    private final Long2ObjectOpenHashMap<PathNode> map;

    protected PathNode startNode;

    protected PathNode mostRecentConsidered;

    protected final PathNode[] bestSoFar = new PathNode[COEFFICIENTS.length];

    private volatile boolean isFinished;

    protected boolean cancelRequested;

    /**
     * This is really complicated and hard to explain. I wrote a comment in the old version of MineBot but it was so
     * long it was easier as a Google Doc (because I could insert charts).
     *
     * @see <a href="https://docs.google.com/document/d/1WVHHXKXFdCR1Oz__KtK8sFqyvSwJN_H4lftkHFgmzlc/edit">here</a>
     */
    protected static final double[] COEFFICIENTS = {1.5, 2, 2.5, 3, 4, 5, 10};

    /**
     * If a path goes less than 5 blocks and doesn't make it to its goal, it's not worth considering.
     */
    protected static final double MIN_DIST_PATH = 5;

    /**
     * there are floating point errors caused by random combinations of traverse and diagonal over a flat area
     * that means that sometimes there's a cost improvement of like 10 ^ -16
     * it's not worth the time to update the costs, decrease-key the heap, potentially repropagate, etc
     * <p>
     * who cares about a hundredth of a tick? that's half a millisecond for crying out loud!
     */
    protected static final double MIN_IMPROVEMENT = 0.01;

    AbstractNodeCostSearch(BlockPos realStart, int startX, int startY, int startZ, Goal goal, CalculationContext context) {
        this.realStart = realStart;
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;
        this.goal = goal;
        this.context = context;
        this.map = new Long2ObjectOpenHashMap<>(1024, 0.75f);
    }

    public void cancel() {
        cancelRequested = true;
    }

    public synchronized PathCalculationResult calculate(long primaryTimeout, long failureTimeout) {
        if (isFinished) {
            throw new IllegalStateException("Path finder cannot be reused!");
        }
        cancelRequested = false;
        try {
            IPath path = calculate0(primaryTimeout, failureTimeout).map(IPath::postProcess).orElse(null);
            if (cancelRequested) {
                return new PathCalculationResult(PathCalculationResult.Type.CANCELLATION);
            }
            if (path == null) {
                return new PathCalculationResult(PathCalculationResult.Type.FAILURE);
            }
            int previousLength = path.length();
            path = path.cutoffAtLoadedChunks();
            if (path.length() < previousLength) {
                PATH_LOG.debug("Cutting off path at edge of loaded chunks");
                PATH_LOG.debug("Length decreased by " + (previousLength - path.length()));
            } else {
                PATH_LOG.debug("Path ends within loaded chunks");
            }
            previousLength = path.length();
            path = path.staticCutoff(goal);
            if (path.length() < previousLength) {
                PATH_LOG.debug("Static cutoff " + previousLength + " to " + path.length());
            }
            if (goal.isInGoal(path.getDest())) {
                return new PathCalculationResult(PathCalculationResult.Type.SUCCESS_TO_GOAL, path);
            } else {
                return new PathCalculationResult(PathCalculationResult.Type.SUCCESS_SEGMENT, path);
            }
        } catch (Exception e) {
            PATH_LOG.info("Pathing exception: ", e);
            return new PathCalculationResult(PathCalculationResult.Type.EXCEPTION);
        } finally {
            // this is run regardless of what exception may or may not be raised by calculate0
            isFinished = true;
        }
    }

    protected abstract Optional<IPath> calculate0(long primaryTimeout, long failureTimeout);

    /**
     * Determines the distance squared from the specified node to the start
     * node. Intended for use in distance comparison, rather than anything that
     * considers the real distance value, hence the "sq".
     *
     * @param n A node
     * @return The distance, squared
     */
    protected double getDistFromStartSq(PathNode n) {
        int xDiff = n.x - startX;
        int yDiff = n.y - startY;
        int zDiff = n.z - startZ;
        return xDiff * xDiff + yDiff * yDiff + zDiff * zDiff;
    }

    /**
     * Attempts to search the block position hashCode long to {@link PathNode} map
     * for the node mapped to the specified pos. If no node is found,
     * a new node is created.
     *
     * @param x        The x position of the node
     * @param y        The y position of the node
     * @param z        The z position of the node
     * @param hashCode The hash code of the node, provided by {@link BlockPos#longHash(int, int, int)}
     * @return The associated node
     * @see <a href="https://github.com/cabaletta/baritone/issues/107">Issue #107</a>
     */
    protected PathNode getNodeAtPosition(int x, int y, int z, long hashCode) {
        PathNode node = map.get(hashCode);
        if (node == null) {
            node = new PathNode(x, y, z, goal);
            map.put(hashCode, node);
        }
        return node;
    }

    public Optional<IPath> pathToMostRecentNodeConsidered() {
        return Optional.ofNullable(mostRecentConsidered).map(node -> new Path(realStart, startNode, node, 0, goal, context));
    }

    public Optional<IPath> bestPathSoFar() {
        return bestSoFar(0);
    }

    public Optional<IPath> bestSoFar(final int numNodes) {
        return bestSoFar(numNodes, MIN_DIST_PATH).or(() -> bestSoFar(numNodes, 1));
    }

    public Optional<IPath> bestSoFar(int numNodes, double minDist) {
        if (startNode == null) {
            return Optional.empty();
        }
        double bestDist = 0;
        for (int i = 0; i < COEFFICIENTS.length; i++) {
            if (bestSoFar[i] == null) {
                continue;
            }
            double dist = getDistFromStartSq(bestSoFar[i]);
            if (dist > bestDist) {
                bestDist = dist;
            }
            if (dist > minDist * minDist) { // square the comparison since distFromStartSq is squared
                return Optional.of(new Path(realStart, startNode, bestSoFar[i], numNodes, goal, context));
            }
        }
        return Optional.empty();
    }

    public final boolean isFinished() {
        return isFinished;
    }

    public final Goal getGoal() {
        return goal;
    }

    public BlockPos getStart() {
        return new BlockPos(startX, startY, startZ);
    }

    protected int mapSize() {
        return map.size();
    }
}
