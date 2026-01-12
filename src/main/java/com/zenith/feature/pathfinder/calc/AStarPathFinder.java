package com.zenith.feature.pathfinder.calc;

import com.zenith.feature.pathfinder.MutableMoveResult;
import com.zenith.feature.pathfinder.calc.openset.BinaryHeapOpenSet;
import com.zenith.feature.pathfinder.goals.Goal;
import com.zenith.feature.pathfinder.movement.ActionCosts;
import com.zenith.feature.pathfinder.movement.CalculationContext;
import com.zenith.feature.pathfinder.movement.Moves;
import com.zenith.feature.pathfinder.util.Favoring;
import com.zenith.feature.player.World;
import com.zenith.mc.block.BlockPos;

import java.util.Optional;

import static com.zenith.Globals.PATH_LOG;

public class AStarPathFinder extends AbstractNodeCostSearch {
    private static final int MAX_MAP_SIZE = 1_000_000;
    private final Favoring favoring;
    private final CalculationContext calcContext;

    public AStarPathFinder(BlockPos realStart, int startX, int startY, int startZ, Goal goal, Favoring favoring, CalculationContext context) {
        super(realStart, startX, startY, startZ, goal, context);
        this.favoring = favoring;
        this.calcContext = context;
    }

    @Override
    protected Optional<IPath> calculate0(long primaryTimeout, long failureTimeout) {
        int minY = World.getCurrentDimension().minY();
        int height = World.getCurrentDimension().height();
        startNode = getNodeAtPosition(startX, startY, startZ, BlockPos.longHash(startX, startY, startZ));
        startNode.cost = 0;
        BinaryHeapOpenSet openSet = new BinaryHeapOpenSet();
        openSet.insert(startNode);
        double[] bestHeuristicSoFar = new double[COEFFICIENTS.length];//keep track of the best node by the metric of (estimatedCostToGoal + cost / COEFFICIENTS[i])
        for (int i = 0; i < bestHeuristicSoFar.length; i++) {
            bestHeuristicSoFar[i] = startNode.estimatedCostToGoal;
            bestSoFar[i] = startNode;
        }
        MutableMoveResult res = new MutableMoveResult();
//        BetterWorldBorder worldBorder = new BetterWorldBorder(calcContext.world.getWorldBorder());
        long startTime = System.currentTimeMillis();
//        boolean slowPath = Baritone.settings().slowPath.value;
//        if (slowPath) {
//            logDebug("slowPath is on, path timeout will be " + Baritone.settings().slowPathTimeoutMS.value + "ms instead of " + primaryTimeout + "ms");
//        }
        long primaryTimeoutTime = startTime + primaryTimeout;
        long failureTimeoutTime = startTime + failureTimeout;
        boolean failing = true;
        int numNodes = 0;
        int numMovementsConsidered = 0;
        int numEmptyChunk = 0;
        boolean isFavoring = !favoring.isEmpty();
        int timeCheckInterval = 1 << 6;
        int pathingMaxChunkBorderFetch = 50;
        double minimumImprovement = MIN_IMPROVEMENT;
        Moves[] allMoves = Moves.values();
        while (!openSet.isEmpty() && numEmptyChunk < pathingMaxChunkBorderFetch && !cancelRequested && mapSize() < MAX_MAP_SIZE) {
            if ((numNodes & (timeCheckInterval - 1)) == 0) { // only call this once every 64 nodes (about half a millisecond)
                long now = System.currentTimeMillis(); // since nanoTime is slow on windows (takes many microseconds)
                if (now - failureTimeoutTime >= 0 || (!failing && now - primaryTimeoutTime >= 0)) {
                    break;
                }
            }
//            if (slowPath) {
//                try {
//                    Thread.sleep(Baritone.settings().slowPathTimeDelayMS.value);
//                } catch (InterruptedException ignored) {}
//            }
            PathNode currentNode = openSet.removeLowest();
            mostRecentConsidered = currentNode;
            numNodes++;
            if (goal.isInGoal(currentNode.x, currentNode.y, currentNode.z)) {
                PATH_LOG.info("Calculated path to goal in {}ms, {} movements considered", System.currentTimeMillis() - startTime, numMovementsConsidered);
                return Optional.of(new Path(realStart, startNode, currentNode, numNodes, goal, calcContext));
            }
            for (int j = 0; j < allMoves.length; j++) {
                final Moves moves = allMoves[j];
                int newX = currentNode.x + moves.xOffset;
                int newZ = currentNode.z + moves.zOffset;
                if ((newX >> 4 != currentNode.x >> 4 || newZ >> 4 != currentNode.z >> 4)
                    && !calcContext.isLoaded(newX, newZ)) {
                    // only need to check if the destination is a loaded chunk if it's in a different chunk than the start of the movement
                    if (!moves.dynamicXZ) { // only increment the counter if the movement would have gone out of bounds guaranteed
                        numEmptyChunk++;
                    }
                    continue;
                }
//                if (!moves.dynamicXZ && !worldBorder.entirelyContains(newX, newZ)) {
//                    continue;
//                }
                if (currentNode.y + moves.yOffset > height || currentNode.y + moves.yOffset < minY) {
                    continue;
                }
                res.reset();
                moves.apply(calcContext, currentNode.x, currentNode.y, currentNode.z, res);
                numMovementsConsidered++;
                double actionCost = res.cost;
                if (actionCost >= ActionCosts.COST_INF) {
                    continue;
                }
                if (actionCost <= 0 || Double.isNaN(actionCost)) {
                    throw new IllegalStateException(moves + " calculated implausible cost " + actionCost);
                }
                // check destination after verifying it's not COST_INF -- some movements return a static IMPOSSIBLE object with COST_INF and destination being 0,0,0 to avoid allocating a new result for every failed calculation
//                if (moves.dynamicXZ && !worldBorder.entirelyContains(res.x, res.z)) { // see issue #218
//                    continue;
//                }
                if (!moves.dynamicXZ && (res.x != newX || res.z != newZ)) {
                    throw new IllegalStateException(moves + " " + res.x + " " + newX + " " + res.z + " " + newZ);
                }
                if (!moves.dynamicY && res.y != currentNode.y + moves.yOffset) {
                    throw new IllegalStateException(moves + " " + res.y + " " + (currentNode.y + moves.yOffset));
                }
                long hashCode = BlockPos.longHash(res.x, res.y, res.z);
                if (isFavoring) {
                    // see issue #18
                    actionCost *= favoring.calculate(hashCode);
                }
                PathNode neighbor = getNodeAtPosition(res.x, res.y, res.z, hashCode);
                double tentativeCost = currentNode.cost + actionCost;
                if (neighbor.cost - tentativeCost > minimumImprovement) {
                    neighbor.previous = currentNode;
                    neighbor.cost = tentativeCost;
                    if (neighbor.isOpen()) {
                        openSet.update(neighbor);
                    } else {
                        openSet.insert(neighbor);
                    }
                    for (int i = 0; i < COEFFICIENTS.length; i++) {
                        double heuristic = neighbor.estimatedCostToGoal + neighbor.cost / COEFFICIENTS[i];
                        if (bestHeuristicSoFar[i] - heuristic > minimumImprovement) {
                            bestHeuristicSoFar[i] = heuristic;
                            bestSoFar[i] = neighbor;
                            if (failing && getDistFromStartSq(neighbor) > MIN_DIST_PATH * MIN_DIST_PATH) {
                                failing = false;
                            }
                        }
                    }
                }
            }
        }
        if (mapSize() > MAX_MAP_SIZE) {
            PATH_LOG.info("Path node map too large: {}", mapSize());
        }
        if (cancelRequested) {
            return Optional.empty();
        }
        PATH_LOG.debug("{} movements considered", numMovementsConsidered);
        PATH_LOG.debug("Open set size: {}", openSet.size());
        PATH_LOG.debug("PathNode map size: {}", mapSize());
        PATH_LOG.debug("{} nodes per second", (int) (numNodes * 1.0 / ((System.currentTimeMillis() - startTime) / 1000F)));
        Optional<IPath> result = bestSoFar(numNodes);
        if (result.isPresent()) {
            PATH_LOG.info("Calculated path in {}ms, goes for: {} blocks, {} movements considered",
                          System.currentTimeMillis() - startTime,
                          String.format("%.2f", result.map(p -> p.getSrc().distance(p.getDest())).orElse(0.0)),
                          numMovementsConsidered);
        } else {
            PATH_LOG.info("No path found in {}ms, {} movements considered", System.currentTimeMillis() - startTime, numMovementsConsidered);
        }

        return result;
    }
}
