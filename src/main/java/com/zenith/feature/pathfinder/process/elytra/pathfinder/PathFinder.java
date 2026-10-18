package com.zenith.feature.pathfinder.process.elytra.pathfinder;

import com.zenith.mc.block.BlockPos;
import com.zenith.mc.block.Direction;

import java.util.*;

/**
 * A* over cubes of the chunk octrees, as the native PathFinder.cpp had it. A node is a cube of 2
 * to 16 blocks that is all air; a step to a neighbour grows the neighbour to the largest empty
 * cube it sits in, or shrinks it into the four smaller cubes on the shared face until they are
 * empty or too small.
 */
final class PathFinder {

    static final double MIN_DIST_PATH = 5; // might want to increase this
    private static final double MIN_IMPROVEMENT = 0.01;
    // The native PathFinder.cpp's order rather than Direction.values(), so the search expands
    // neighbours as it always has.
    private static final Direction[] ALL_FACES = {Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    static final class Path {
        enum Type {
            SEGMENT,
            FINISHED
        }

        final Type type;
        final BlockPos start;
        final BlockPos goal; // where the path wants to go, not necessarily where it ends
        final List<BlockPos> blocks;

        Path(Type type, BlockPos start, BlockPos goal, List<BlockPos> blocks) {
            this.type = type;
            this.start = start;
            this.goal = goal;
            this.blocks = blocks;
        }

        BlockPos getEndPos() {
            // this should basically never be empty
            return !this.blocks.isEmpty() ? this.blocks.get(this.blocks.size() - 1) : this.start;
        }
    }

    private PathFinder() {}

    private static PathNode getNodeAtPosition(Map<NodePos, PathNode> map, NodePos pos, BlockPos goal) {
        var node = map.get(pos);
        if (node == null) {
            node = new PathNode(pos, goal);
            map.put(pos, node);
        }
        return node;
    }

    private static Path createPath(PathNode end, BlockPos startPos, BlockPos goal, Path.Type type) {
        final List<BlockPos> blocks = new ArrayList<>();
        for (PathNode current = end; current != null; current = current.previous) {
            blocks.add(current.pos.absolutePosCenter());
        }
        Collections.reverse(blocks);
        return new Path(type, startPos, goal, blocks);
    }

    static boolean isInBounds(int worldHeight, int y) {
        return y >= 0 && y < worldHeight;
    }

    private static boolean closeToGoal(NodePos node, BlockPos goal) {
        return node.centerDistSqr(goal) <= 16 * 16;
    }

    private static boolean inGoal(NodePos node, BlockPos goal) {
        if (!closeToGoal(node, goal)) return false;
        final int x = node.minX(), y = node.minY(), z = node.minZ();
        final int w = node.size.width();
        return goal.x() >= x && goal.x() <= x + w &&
                goal.y() >= y && goal.y() <= y + w &&
                goal.z() >= z && goal.z() <= z + w;
    }

    private static Path bestPathSoFar(PathNode end, BlockPos startPos, BlockPos goal) {
        final double distSq = end.pos.centerDistSqr(startPos);
        if (distSq > MIN_DIST_PATH * MIN_DIST_PATH) {
            return createPath(end, startPos, goal, Path.Type.SEGMENT);
        }
        // the path took too long and got nowhere
        return null;
    }

    // ---- neighbour expansion ----

    private interface Callback {
        void accept(NodePos neighbor, Chunk chunk, boolean fromCaller);
    }

    /**
     * Called inside a big neighbour cube whose lowest corner is (x, y, z); iterates the 4 sub cubes
     * that are adjacent to the original cube: the corner of the layer that touches it, one step
     * along each of the layer's two axes, and both. The face is relative to the original cube, the
     * size is the sub cubes'.
     */
    private static void forEachSubCube(Chunk chunk, boolean fromCaller, int x, int y, int z, Direction face, Size sz, Size minSize, Callback callback) {
        final int w = sz.width();
        int ax = 0, ay = 0, az = 0, bx = 0, by = 0, bz = 0;
        switch (face) {
            case UP:
                ax = w; bz = w;
                break;
            case DOWN:
                y += w; ax = w; bz = w;
                break;
            case NORTH:
                z += w; ax = w; by = w;
                break;
            case SOUTH:
                ax = w; by = w;
                break;
            case EAST:
                az = w; by = w;
                break;
            default: // WEST
                x += w; az = w; by = w;
                break;
        }
        forEachNeighborInCube(chunk, fromCaller, new NodePos(sz, x, y, z), face, sz, false, minSize, callback);
        forEachNeighborInCube(chunk, fromCaller, new NodePos(sz, x + ax, y + ay, z + az), face, sz, false, minSize, callback);
        forEachNeighborInCube(chunk, fromCaller, new NodePos(sz, x + bx, y + by, z + bz), face, sz, false, minSize, callback);
        forEachNeighborInCube(chunk, fromCaller, new NodePos(sz, x + ax + bx, y + ay + by, z + az + bz), face, sz, false, minSize, callback);
    }

    private static void forEachNeighborInCube(Chunk chunk, boolean fromCaller, NodePos neighborNode, Direction face, Size size, boolean sizeChange, Size minSize, Callback callback) {
        if (sizeChange) {
            callback.accept(neighborNode, chunk, fromCaller);
            return;
        }
        final int x = neighborNode.minX(), y = neighborNode.minY(), z = neighborNode.minZ();
        if (chunk.isEmpty(size, x & 15, y, z & 15)) {
            callback.accept(neighborNode, chunk, fromCaller);
            return;
        }
        if (size != Size.X1) {
            var nextSize = size.smaller();
            // Don't shrink cubes to X1 because they suck and make the path try to squeeze through small areas
            if (nextSize.ordinal() < minSize.ordinal()) return;
            forEachSubCube(chunk, fromCaller, x, y, z, face, nextSize, minSize, callback);
        }
    }

    /** Grows the neighbour to the largest empty cube it is in, then iterates what is on the face. */
    private static void growThenIterate(Chunk chunk, boolean fromCaller, NodePos pos, Direction face, Size minSize, Callback callback) {
        var originalSize = pos.size;
        final int x = pos.minX(), y = pos.minY(), z = pos.minZ();
        for (Size s = originalSize; ; s = s.larger()) {
            if (s == Size.X16 || !chunk.isEmpty(s.larger(), x & 15, y, z & 15)) {
                forEachNeighborInCube(chunk, fromCaller, new NodePos(s, x, y, z), face, s, originalSize != s, minSize, callback);
                return;
            }
        }
    }

    // ---- the search ----

    private static final class Search implements Callback {
        final Map<NodePos, PathNode> map = new HashMap<>();
        final BinaryHeapOpenSet openSet = new BinaryHeapOpenSet();
        final BlockPos goalCenter;
        final BlockPos startCenter;
        final double fakeChunkCost;
        PathNode currentNode;
        PathNode bestSoFar;
        double bestHeuristicSoFar;
        boolean failing = true;

        Search(BlockPos goalCenter, BlockPos startCenter, double fakeChunkCost) {
            this.goalCenter = goalCenter;
            this.startCenter = startCenter;
            this.fakeChunkCost = fakeChunkCost;
        }

        @Override
        public void accept(NodePos neighborPos, Chunk chunk, boolean fromCaller) {
            var neighborNode = getNodeAtPosition(this.map, neighborPos, this.goalCenter);
            final double cost = fromCaller ? 1 : this.fakeChunkCost;
            final double tentativeCost = this.currentNode.cost + cost;
            if (neighborNode.cost - tentativeCost > MIN_IMPROVEMENT) {
                neighborNode.previous = this.currentNode;
                neighborNode.cost = tentativeCost;
                neighborNode.combinedCost = tentativeCost + neighborNode.estimatedCostToGoal;

                if (neighborNode.isOpen()) {
                    this.openSet.update(neighborNode);
                } else {
                    this.openSet.insert(neighborNode); // dont double count, dont insert into open set if it's already there
                }

                final double heuristic = neighborNode.combinedCost;
                if (this.bestHeuristicSoFar - heuristic > MIN_IMPROVEMENT) {
                    this.bestHeuristicSoFar = heuristic;
                    this.bestSoFar = neighborNode;
                    if (this.failing && neighborPos.centerDistSqr(this.startCenter) > MIN_DIST_PATH * MIN_DIST_PATH) {
                        this.failing = false;
                    }
                }
            }
        }
    }

    /**
     * One segment of a path from start towards goal. Returns a FINISHED path if it reached the
     * goal, a SEGMENT if it ran out of time but got somewhere, and null if it got nowhere or was
     * cancelled.
     */
    static Path findPathSegment(NetherPathfinder ctx, NodePos start, NodePos goal, boolean x4Min, int timeoutMs, boolean airIfFake, double fakeChunkCost) {
        final NetherPathfinder.CacheMiss fakeChunkMode = airIfFake ? NetherPathfinder.CacheMiss.AIR : NetherPathfinder.CacheMiss.SOLID;
        var minSize = x4Min ? Size.X4 : Size.X2;
        var goalCenter = goal.absolutePosCenter();
        var startCenter = start.absolutePosCenter();

        var s = new Search(goalCenter, startCenter, fakeChunkCost);

        var startNode = getNodeAtPosition(s.map, start, new BlockPos(goal.minX(), goal.minY(), goal.minZ()));
        startNode.cost = 0;
        startNode.combinedCost = startNode.estimatedCostToGoal;
        s.openSet.insert(startNode);
        ctx.getRealChunkOrDefault((start.minX() >> 4), (start.minZ() >> 4), fakeChunkMode);

        s.bestSoFar = startNode;
        s.bestHeuristicSoFar = startNode.estimatedCostToGoal;

        final long startTime = System.currentTimeMillis();
        final long primaryTimeoutTime = startTime + 500L;
        final long timeout = timeoutMs != 0 ? timeoutMs : 30_000L;
        final long failureTimeout = startTime + timeout;

        int numNodes = 0;
        int fakeChunkVisits = 0; // if this gets too high we return
        while (!s.openSet.isEmpty()) {
            if ((numNodes & 63) == 0) { // only look at the clock once every 64 nodes
                final long now = System.currentTimeMillis();
                if (now >= failureTimeout || (!s.failing && now >= primaryTimeoutTime)) {
                    break;
                } else if (ctx.isCancelled()) {
                    return null;
                }
            }
            numNodes++;

            var currentNode = s.openSet.removeLowest();
            s.currentNode = currentNode;
            if (inGoal(currentNode.pos, goalCenter)) {
                return createPath(currentNode, startCenter, goalCenter, Path.Type.FINISHED);
            }
            var pos = currentNode.pos;
            var size = pos.size;
            final int x = pos.minX(), y = pos.minY(), z = pos.minZ();
            final int w = size.width();
            final int cx = (x >> 4);
            final int cz = (z >> 4);
            final NetherPathfinder.Entry currentChunk = ctx.getChunkOrAir(cx, cz);
            if (!currentChunk.fromCaller) {
                fakeChunkVisits++;
            } else {
                fakeChunkVisits = 0;
            }
            if (fakeChunkVisits >= 100 && airIfFake) {
                return bestPathSoFar(s.bestSoFar, startCenter, goalCenter);
            }

            for (Direction face : ALL_FACES) {
                final int nx = x + face.x() * w;
                final int ny = y + face.y() * w;
                final int nz = z + face.z() * w;
                if (face == Direction.UP || face == Direction.DOWN) {
                    if (!isInBounds(ctx.maxHeight, ny)) continue;
                }
                final int neighborCx = (nx >> 4);
                final int neighborCz = (nz >> 4);
                final NetherPathfinder.Entry entry = neighborCx == cx && neighborCz == cz ? currentChunk : ctx.getChunkOrAir(neighborCx, neighborCz);
                growThenIterate(entry.chunk, entry.fromCaller, new NodePos(size, nx, ny, nz), face, minSize, s);
            }
        }
        return bestPathSoFar(s.bestSoFar, startCenter, goalCenter);
    }

    /** The nearest cube of the given size around block (x, y, z) that is all air, searching outwards. */
    static NodePos findAir(NetherPathfinder ctx, Size size, int x, int y, int z, boolean airIfFake) {
        if (!isInBounds(ctx.maxHeight, y)) {
            throw new IllegalArgumentException("position is out of bounds: " + x + ", " + y + ", " + z);
        }
        var start = new NodePos(size, x, y, z);
        final ArrayDeque<NodePos> queue = new ArrayDeque<>();
        final Set<NodePos> visited = new HashSet<>();
        queue.add(start);
        visited.add(start);
        final int w = size.width();
        while (!queue.isEmpty()) {
            if (ctx.isCancelled() || Thread.currentThread().isInterrupted()) return null;
            var node = queue.poll();
            final int nx = node.minX(), ny = node.minY(), nz = node.minZ();
            if (isInBounds(ctx.maxHeight, ny)) {
                var chunk = airIfFake
                        ? ctx.getChunkOrAir((nx >> 4), (nz >> 4)).chunk
                        : ctx.getChunkOrDefault((nx >> 4), (nz >> 4), true);
                if (chunk.isEmpty(size, nx & 15, ny, nz & 15)) {
                    return node;
                }
                push(queue, visited, new NodePos(size, nx - w, ny, nz)); // west
                push(queue, visited, new NodePos(size, nx + w, ny, nz)); // east
                push(queue, visited, new NodePos(size, nx, ny, nz - w)); // north
                push(queue, visited, new NodePos(size, nx, ny, nz + w)); // south
                push(queue, visited, new NodePos(size, nx, ny + w, nz)); // above
                push(queue, visited, new NodePos(size, nx, ny - w, nz)); // below
            }
        }
        // shouldn't be possible to exit the while loop
        throw new IllegalStateException("no air anywhere around " + x + ", " + y + ", " + z);
    }

    private static void push(ArrayDeque<NodePos> queue, Set<NodePos> visited, NodePos pos) {
        if (visited.add(pos)) queue.add(pos);
    }
}
