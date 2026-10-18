package com.zenith.feature.pathfinder.process.elytra;

import com.zenith.cache.data.entity.Entity;
import com.zenith.feature.inventory.InventoryActionRequest;
import com.zenith.feature.inventory.actions.ClickItem;
import com.zenith.feature.pathfinder.Baritone;
import com.zenith.feature.pathfinder.PathInput;
import com.zenith.feature.pathfinder.PlayerContext;
import com.zenith.feature.pathfinder.process.ElytraProcess;
import com.zenith.feature.pathfinder.process.ProcessUtil;
import com.zenith.feature.pathfinder.util.RotationUtils;
import com.zenith.feature.player.Rotation;
import com.zenith.feature.player.World;
import com.zenith.feature.player.raycast.RaycastHelper;
import com.zenith.mc.block.BlockPos;
import com.zenith.mc.block.BlockRegistry;
import com.zenith.mc.block.LocalizedCollisionBox;
import com.zenith.mc.dimension.DimensionRegistry;
import com.zenith.mc.item.ItemRegistry;
import com.zenith.util.ItemUtil;
import com.zenith.util.RequestFuture;
import com.zenith.util.struct.Pair;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatIterator;
import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ClickItemAction;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.UnaryOperator;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.INVENTORY;
import static com.zenith.util.math.MathHelper.ceilI;
import static com.zenith.util.math.MathHelper.floorI;

public final class ElytraBehavior implements ProcessUtil {
    private final Baritone baritone;
    private final PlayerContext ctx;

    // Render stuff
    private final List<Pair<Vector3d, Vector3d>> clearLines;
    private final List<Pair<Vector3d, Vector3d>> blockedLines;
    private List<Vector3d> simulationLine;
    private BlockPos aimPos;
    private List<BlockPos> visiblePath;

    // :sunglasses:
    public NetherPathfinderContext npfContext;
    public IElytraPathFinder pathFinder;

    public final PathManager pathManager;
    private final ElytraProcess process;

    /**
     * Remaining cool-down ticks between firework usage
     */
    private int remainingFireworkTicks;

    /**
     * Remaining cool-down ticks after the player's position and rotation are reset by the server
     */
    private int remainingSetBackTicks;

    public volatile boolean landingMode;
    public volatile boolean initialPathFailed;

    /**
     * The most recent minimum number of firework boost ticks, equivalent to {@code 10 * (1 + Flight)}
     * <p>
     * Updated every time a firework is automatically used
     */
    private int minimumBoostTicks;

    private boolean deployedFireworkLastTick;
    private final int[] nextTickBoostCounter;

    public final BlockPos destination;
    private final boolean appendDestination;

    private final ExecutorService solverExecutor;
    private Future<Solution> solver;
    private Solution pendingSolution;
    private boolean solveNextTick;

    private long timeLastCacheCull = 0L;

    // auto swap
    private RequestFuture inventoryRequest;
    private final Queue<Runnable> tickTasks = new ConcurrentLinkedQueue<>();
    private volatile boolean destroyed;
    private volatile boolean setback;
    public boolean useFirework;
    public Hand fireworkHand = Hand.MAIN_HAND;
    private int requestedBoostTicks;

    public ElytraBehavior(Baritone baritone, ElytraProcess process, NetherPathfinderContext npf, BlockPos destination, boolean appendDestination) {
        this.baritone = baritone;
        this.ctx = baritone.getPlayerContext();
        this.clearLines = new CopyOnWriteArrayList<>();
        this.blockedLines = new CopyOnWriteArrayList<>();
        this.pathManager = this.new PathManager();
        this.process = process;
        this.destination = new BlockPos(destination);
        this.appendDestination = appendDestination;
        this.solverExecutor = Executors.newSingleThreadExecutor(Thread.ofPlatform().daemon().name("Elytra Solver").factory());
        this.nextTickBoostCounter = new int[2];

        this.npfContext = npf;

        if(World.getCurrentDimension() == DimensionRegistry.THE_NETHER.get()) {
            this.pathFinder = false /* Baritone.settings().elytraAllowAboveRoof */ && true /* Baritone.settings().elytraAllowAboveBuildLimit */
                    ? new BuildLimitPathFinder(ctx, npfContext)
                    : npfContext;
        } else {
            this.pathFinder = true /* Baritone.settings().elytraAllowAboveBuildLimit */ ? new BuildLimitPathFinder(ctx, npfContext) : npfContext;
        }
    }

    public final class PathManager {

        public volatile NetherPath path;
        private volatile boolean completePath;
        private volatile boolean recalculating;

        private int maxPlayerNear;
        private int ticksNearUnchanged;
        private int playerNear;

        public PathManager() {
            // lol imagine initializing fields normally
            this.clear();
        }

        public void tick() {
            // Recalculate closest path node
            this.updatePlayerNear();
            final int prevMaxNear = this.maxPlayerNear;
            this.maxPlayerNear = Math.max(this.maxPlayerNear, this.playerNear);

            if (this.maxPlayerNear == prevMaxNear && ctx.player().isFallFlying()) {
                this.ticksNearUnchanged++;
            } else {
                this.ticksNearUnchanged = 0;
            }

            npfContext.acquireReadLock();
            try {
                // Obstacles are more important than an incomplete path, handle those first.
                this.pathfindAroundObstacles();
            } finally {
                npfContext.releaseReadLock();
            }
            this.attemptNextSegment();

        }

        public CompletableFuture<Void> pathToDestination() {
            return this.pathToDestination(ctx.playerFeet());
        }

        public CompletableFuture<Void> pathToDestination(final BlockPos from) {
            this.recalculating = true;
            final long start = System.nanoTime();
            return this.path0(from, destinationFixed(), UnaryOperator.identity())
                    .thenRun(() -> {
                        final double distance = this.path.get(0).distance(this.path.get(this.path.size() - 1));
                        if (this.completePath) {
                            logVerbose(String.format("Computed path (%.1f blocks in %.4f seconds)", distance, (System.nanoTime() - start) / 1e9d));
                        } else {
                            logVerbose(String.format("Computed segment (Next %.1f blocks in %.4f seconds)", distance, (System.nanoTime() - start) / 1e9d));
                        }
                    })
                    .whenComplete((result, ex) -> {
                        this.recalculating = false;
                        if (ex != null) {
                            initialPathFailed = true;
                            final Throwable cause = ex.getCause();
                            if (cause instanceof PathCalculationException) {
                                info("Failed to compute path to destination");
                            } else {
                                error("Elytra path calculation failed", cause);
                            }
                        }
                    });
        }

        public CompletableFuture<Void> pathRecalcSegment(final OptionalInt upToIncl) {
            if (this.recalculating) {
                throw new IllegalStateException("already recalculating");
            }

            this.recalculating = true;
            final List<BlockPos> after = upToIncl.isPresent() ? this.path.subList(upToIncl.getAsInt() + 1, this.path.size()) : Collections.emptyList();
            final boolean complete = this.completePath;

            return this.path0(ctx.playerFeet(), upToIncl.isPresent() ? fixDestination(this.path.get(upToIncl.getAsInt())) : destinationFixed(), segment -> segment.append(after.stream(), complete || (segment.isFinished() && !upToIncl.isPresent())))
                    .whenComplete((result, ex) -> {
                        this.recalculating = false;
                        if (ex != null) {
                            final Throwable cause = ex.getCause();
                            if (cause instanceof PathCalculationException) {
                                info("Failed to recompute segment");
                            } else {
                                error("Elytra path calculation failed", cause);
                            }
                        }
                    });
        }

        public void pathNextSegment(final int afterIncl) {
            if (this.recalculating) {
                return;
            }

            this.recalculating = true;
            final List<BlockPos> before = this.path.subList(0, afterIncl + 1);
            final long start = System.nanoTime();
            var pathStart = this.path.get(afterIncl);

            this.path0(pathStart, destinationFixed(), segment -> segment.prepend(before.stream()))
                    .thenRun(() -> {
                        final int recompute = this.path.size() - before.size() - 1;
                        final double distance = recompute > 0 ? this.path.get(0).distance(this.path.get(recompute)) : 0;

                        if (this.completePath) {
                            logVerbose(String.format("Computed path (%.1f blocks in %.4f seconds)", distance, (System.nanoTime() - start) / 1e9d));
                        } else {
                            logVerbose(String.format("Computed segment (Next %.1f blocks in %.4f seconds)", distance, (System.nanoTime() - start) / 1e9d));
                        }
                    })
                    .whenComplete((result, ex) -> {
                        this.recalculating = false;
                        if (ex != null) {
                            final Throwable cause = ex.getCause();
                            if (cause instanceof PathCalculationException) {
                                info("Failed to compute next segment");
                                if (ctx.playerFeetAsVec().distanceSquared(Vector3d.from(pathStart.x() + 0.5, pathStart.y() + 0.5, pathStart.z() + 0.5)) < 16 * 16) {
                                    logVerbose("Player is near the segment start, therefore repeating this calculation is pointless. Marking as complete");
                                    completePath = true;
                                }
                            } else {
                                error("Elytra path calculation failed", cause);
                            }
                        }
                    });
        }

        public void clear() {
            this.path = NetherPath.emptyPath();
            this.completePath = true;
            this.recalculating = false;
            this.playerNear = 0;
            this.ticksNearUnchanged = 0;
            this.maxPlayerNear = 0;
        }

        private void setPath(final UnpackedSegment segment) {
            if (destroyed) return;
            List<BlockPos> path = segment.collect();
            if (path.isEmpty()) throw new PathCalculationException("Empty elytra path");
            if (ElytraBehavior.this.appendDestination) {
                var dest = destinationFixed();
                var last = !path.isEmpty() ? path.get(path.size() - 1) : null;
                if (last != null && ElytraBehavior.this.clearView(Vector3d.from(dest.x(), dest.y(), dest.z()), Vector3d.from(last.x(), last.y(), last.z()), false)) {
                    path.add(new BlockPos(dest));
                } else {
                    info("unable to land at " + dest);
                    process.landingSpotIsBad(new BlockPos(dest));
                }
            }
            this.path = new NetherPath(path);
            this.completePath = segment.isFinished();
            this.playerNear = 0;
            this.ticksNearUnchanged = 0;
            this.maxPlayerNear = 0;
        }

        public NetherPath getPath() {
            return this.path;
        }

        public int getNear() {
            return this.playerNear;
        }

        // mickey resigned
        private CompletableFuture<Void> path0(BlockPos src, BlockPos dst, UnaryOperator<UnpackedSegment> operator) {
            return ElytraBehavior.this.pathFinder.pathFindAsync(src, dst)
                    .thenApply(operator)
                    .thenAcceptAsync(segment -> {
                        npfContext.acquireReadLock();
                        try {
                            this.setPath(segment);
                        } finally {
                            npfContext.releaseReadLock();
                        }
                    }, task -> { if (!destroyed) tickTasks.add(task); });
        }

        // required read lock to be held
        private void pathfindAroundObstacles() {
            if (this.recalculating) {
                return;
            }

            int rangeStartIncl = playerNear;
            int rangeEndExcl = playerNear;
            while (rangeEndExcl < path.size() && npfContext.hasChunk(path.get(rangeEndExcl))) {
                rangeEndExcl++;
            }
            // rangeEndExcl now represents an index either not in the path, or just outside render distance
            if (rangeStartIncl >= rangeEndExcl) {
                // not loaded yet?
                return;
            }
            var rangeStart = path.get(rangeStartIncl);
            if (!ElytraBehavior.this.passable(rangeStart.x(), rangeStart.y(), rangeStart.z(), false)) {
                // we're in a wall
                return; // previous iterations of this function SHOULD have fixed this by now :rage_cat:
            }

            if (ElytraBehavior.this.process.state != ElytraProcess.State.LANDING && this.ticksNearUnchanged > 100) {
                this.pathRecalcSegment(OptionalInt.of(rangeEndExcl - 1))
                        .thenRun(() -> {
                            logVerbose("Recalculating segment, no progress in last 100 ticks");
                        });
                this.ticksNearUnchanged = 0;
                return;
            }

            boolean canSeeAny = false;
            for (int i = rangeStartIncl; i < rangeEndExcl - 1; i++) {
                if (ElytraBehavior.this.clearView(ctx.playerFeetAsVec(), this.path.getVec(i), false) || ElytraBehavior.this.clearView(ctx.playerHead(), this.path.getVec(i), false)) {
                    canSeeAny = true;
                }
                if (!ElytraBehavior.this.clearView(this.path.getVec(i), this.path.getVec(i + 1), false)) {
                    // obstacle. where do we return to pathing?
                    // if the end of render distance is closer to goal, then that's fine, otherwise we'd be "digging our hole deeper" and making an already bad backtrack worse
                    OptionalInt rejoinMainPathAt;
                    var dest = destinationFixed();
                    if (this.path.get(rangeEndExcl - 1).squaredDistance(dest) < ctx.playerFeet().squaredDistance(dest)) {
                        rejoinMainPathAt = OptionalInt.of(rangeEndExcl - 1); // rejoin after current render distance
                    } else {
                        rejoinMainPathAt = OptionalInt.empty(); // large backtrack detected. ignore render distance, rejoin later on
                    }

                    var blockage = this.path.get(i);
                    final double distance = ctx.playerFeet().distance(this.path.get(rejoinMainPathAt.orElse(path.size() - 1)));

                    final long start = System.nanoTime();
                    this.pathRecalcSegment(rejoinMainPathAt)
                            .thenRun(() -> {
                                logVerbose(String.format("Recalculated segment around path blockage near %s %s %s (next %.1f blocks in %.4f seconds)",
                                        blockage.x(),
                                        blockage.y(),
                                        blockage.z(),
                                        distance,
                                        (System.nanoTime() - start) / 1e9d
                                ));
                            });
                    return;
                }
            }
            if (!canSeeAny && rangeStartIncl < rangeEndExcl - 2) {
                this.pathRecalcSegment(OptionalInt.of(rangeEndExcl - 1)).thenRun(() -> logVerbose("Recalculated segment since no path points were visible"));
            }
        }

        private void attemptNextSegment() {
            if (this.recalculating) {
                return;
            }

            final int last = this.path.size() - 1;
            if (last < 0) return;
            var lastPos = this.path.get(last);
            // Check the chunk independently of Y so paths above the build limit can advance.
            if (!this.completePath && World.isChunkLoadedChunkPos(lastPos.x() >> 4,lastPos.z() >> 4)) {
                this.pathNextSegment(last);
            }
        }

        public void updatePlayerNear() {
            if (this.path.isEmpty()) {
                return;
            }

            int index = this.playerNear;
            var pos = ctx.playerFeet();
            for (int i = index; i >= Math.max(index - 1000, 0); i -= 10) {
                if (path.get(i).squaredDistance(pos) < path.get(index).squaredDistance(pos)) {
                    index = i; // intentional: this changes the bound of the loop
                }
            }
            for (int i = index; i < Math.min(index + 1000, path.size()); i += 10) {
                if (path.get(i).squaredDistance(pos) < path.get(index).squaredDistance(pos)) {
                    index = i; // intentional: this changes the bound of the loop
                }
            }
            for (int i = index; i >= Math.max(index - 50, 0); i--) {
                if (path.get(i).squaredDistance(pos) < path.get(index).squaredDistance(pos)) {
                    index = i; // intentional: this changes the bound of the loop
                }
            }
            for (int i = index; i < Math.min(index + 50, path.size()); i++) {
                if (path.get(i).squaredDistance(pos) < path.get(index).squaredDistance(pos)) {
                    index = i; // intentional: this changes the bound of the loop
                }
            }
            this.playerNear = index;
        }

        public boolean isComplete() {
            return this.completePath;
        }
    }

    public void onReceivePacket() {
        this.setback = true;
    }

    public void pathTo() {
        this.pathManager.pathToDestination();
    }

    public void cancel() {
        this.destroyed = true;
        this.tickTasks.clear();
        if (this.solver != null) {
            this.solver.cancel(true);
        }
        this.solverExecutor.shutdownNow();
    }

    public void destroy() {
        cancel();
        try {
            while (!this.solverExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {}
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void onTick() {
        this.useFirework = false;
        Runnable task;
        while ((task = tickTasks.poll()) != null) {
            if (!destroyed) task.run();
        }
        if (setback) {
            remainingSetBackTicks = 15; // Baritone.settings().elytraFireworkSetbackUseDelay
            setback = false;
        }
        if (npfContext.tryAcquireReadLock()) {
            try {
                this.onTick0();
            } finally {
                npfContext.releaseReadLock();
            }
        }
        final long now = System.currentTimeMillis();
        if ((now - this.timeLastCacheCull) / 1000 > TimeUnit.MINUTES.toSeconds(3) /* Baritone.settings().elytraTimeBetweenCacheCullSecs */) {
            npfContext.queueCacheCulling(ctx.playerFeet().getChunkX(), ctx.playerFeet().getChunkZ(), 5000 /* Baritone.settings().elytraCacheCullDistance */);
            this.timeLastCacheCull = now;
        }
    }

    private void onTick0() {
        // Fetch the previous solution, regardless of if it's going to be used
        this.pendingSolution = null;
        if (this.solver != null) {
            if (this.solver.isDone()) {
                try {
                    this.pendingSolution = this.solver.get();
                } catch (Exception ignored) {
                    // it doesn't matter if get() fails since the solution can just be recalculated synchronously
                } finally {
                    this.solver = null;
                }
            } else {
                // avoid wasting more cycles on a hard solution, we'll do the work synchronously
                this.solver.cancel(true);
                this.solver = null;
            }
        }

        tickInventoryTransactions();

        // Certified mojang employee incident
        if (this.remainingFireworkTicks > 0) {
            this.remainingFireworkTicks--;
        }
        if (this.remainingSetBackTicks > 0) {
            this.remainingSetBackTicks--;
        }
        if (!this.getAttachedFirework().isPresent()) {
            this.minimumBoostTicks = 0;
        }

        // Reset rendered elements
        this.clearLines.clear();
        this.blockedLines.clear();
        this.visiblePath = null;
        this.simulationLine = null;
        this.aimPos = null;

        final List<BlockPos> path = this.pathManager.getPath();
        if (path.isEmpty()) {
            return;
        } else if (this.destination == null) { // null check why????
            this.pathManager.clear();
            return;
        }

        // ctx AND context???? :DDD
        this.pathManager.tick();

        final int playerNear = this.pathManager.getNear();
        this.visiblePath = path.subList(
                Math.max(playerNear - 30, 0),
                Math.min(playerNear + 100, path.size())
        );
    }

    /**
     * Called by {@link ElytraProcess#onTick(boolean, boolean)} when the process is in control and the player is flying
     */
    public void tick() {
        if (this.pathManager.getPath().isEmpty()) {
            return;
        }
        trySwapElytra();

        if (ctx.player().isHorizontalCollision()) {
            logVerbose("hbonk");
        }
        if (ctx.player().isVerticalCollision()) {
            logVerbose("vbonk");
        }

        var solverContext = this.new SolverContext(false);
        this.solveNextTick = true;

        // If there's no previously calculated solution to use, or the context used at the end of last tick doesn't match this tick
        final Solution solution;
        if (this.pendingSolution != null && this.pendingSolution.context.equals(solverContext)) {
            solution = this.pendingSolution;
        } else {
            solution = this.solveAngles(solverContext);
        }

        if (this.deployedFireworkLastTick) {
            this.nextTickBoostCounter[solverContext.boost.isBoosted() ? 1 : 0]++;
            this.deployedFireworkLastTick = false;
        }

        final boolean inLava = ctx.player().isTouchingLava();
        if (inLava) {
            baritone.getInputOverrideHandler().setInputForceState(PathInput.JUMP, true);
        }

        if (solution == null) {
            logVerbose("no solution");
            return;
        }

        baritone.getLookBehavior().updateRotation(solution.rotation);

        if (!solution.solvedPitch) {
            logVerbose("no pitch solution, probably gonna crash in a few ticks LOL!!!");
            return;
        } else {
            this.aimPos = new BlockPos(solution.goingTo.getX(), solution.goingTo.getY(), solution.goingTo.getZ());
        }

        this.tickUseFireworks(
                solution.context.start,
                solution.goingTo,
                solution.context.boost.isBoosted(),
                solution.forceUseFirework || inLava
        );
    }

    public void onPostTick() {
        if (!destroyed && this.solveNextTick) {
            // We're at the end of the tick, the player's position likely updated and the closest path node could've
            // changed. Updating it now will avoid unnecessary recalculation on the main thread.
            this.pathManager.updatePlayerNear();

            var context = this.new SolverContext(true);
            this.solver = this.solverExecutor.submit(() -> {
                npfContext.acquireReadLock();
                try {
                    return this.solveAngles(context);
                } finally {
                    npfContext.releaseReadLock();
                }
            });
            this.solveNextTick = false;
        }
    }

    // calls passable which requires a read lock
    private Solution solveAngles(final SolverContext context) {
        final NetherPath path = context.path;
        final int playerNear = landingMode ? path.size() - 1 : context.playerNear;
        var start = context.start;
        Solution solution = null;

        for (int relaxation = 0; relaxation < 3; relaxation++) { // try for a strict solution first, then relax more and more (if we're in a corner or near some blocks, it will have to relax its constraints a bit)
            int[] heights = context.boost.isBoosted() ? new int[]{20, 10, 5, 0} : new int[]{0}; // attempt to gain height, if we can, so as not to waste the boost
            int lookahead = relaxation == 0 ? 2 : 3; // ideally this would be expressed as a distance in blocks, rather than a number of voxel steps
            //int minStep = Math.max(0, playerNear - relaxation);
            int minStep = playerNear;

            for (int i = Math.min(playerNear + 20, path.size() - 1); i >= minStep; i--) {
                if (Thread.currentThread().isInterrupted()) return null; // cancelled by the game thread
                final List<Pair<Vector3d, Integer>> candidates = new ArrayList<>();
                for (int dy : heights) {
                    if (relaxation == 0 || i == minStep) {
                        // no interp
                        candidates.add(new Pair<>(path.getVec(i), dy));
                    } else if (relaxation == 1) {
                        final double[] interps = new double[]{1.0, 0.75, 0.5, 0.25};
                        for (double interp : interps) {
                            var dest = interp == 1.0
                                    ? path.getVec(i)
                                    : path.getVec(i).mul(interp).add(path.getVec(i - 1).mul(1.0 - interp));
                            candidates.add(new Pair<>(dest, dy));
                        }
                    } else {
                        // Create a point along the segment every block
                        var delta = path.getVec(i).sub(path.getVec(i - 1));
                        final int steps = floorI(delta.length());
                        var step = delta.lengthSquared() < 1.0E-8 ? Vector3d.ZERO : delta.normalize();
                        var stepped = path.getVec(i);
                        for (int interp = 0; interp < steps; interp++) {
                            candidates.add(new Pair<>(stepped, dy));
                            stepped = stepped.sub(step);
                        }
                    }
                }

                for (final Pair<Vector3d, Integer> candidate : candidates) {
                    final Integer augment = candidate.right();
                    var dest = candidate.left().add(0, augment, 0);
                    if (landingMode) {
                        dest = dest.add(0.5, 0.5, 0.5);
                    }

                    if (augment != 0) {
                        if (i + lookahead >= path.size()) {
                            continue;
                        }
                        if (start.distance(dest) < 40) {
                            if (!this.clearView(dest, path.getVec(i + lookahead).add(0, augment, 0), false)
                                    || !this.clearView(dest, path.getVec(i + lookahead), false)) {
                                // aka: don't go upwards if doing so would prevent us from being able to see the next position **OR** the modified next position
                                continue;
                            }
                        } else {
                            // but if it's far away, allow gaining altitude if we could lose it again by the time we get there
                            if (!this.clearView(dest, path.getVec(i), false)) {
                                continue;
                            }
                        }
                    }

                    final double minAvoidance = 0.2 /* Baritone.settings().elytraMinimumAvoidance */;
                    final Double growth = relaxation == 2 ? null
                            : relaxation == 0 ? 2 * minAvoidance : minAvoidance;

                    if (this.isHitboxClear(context, dest, growth)) {
                        // Yaw is trivial, just calculate the rotation required to face the destination
                        final float yaw = RotationUtils.calcRotationFromVec3d(start, dest, ctx.playerRotations()).yaw();

                        final Pair<Float, Boolean> pitch = this.solvePitch(context, dest, relaxation);
                        if (pitch == null) {
                            solution = new Solution(context, new Rotation(yaw, ctx.playerRotations().pitch()), null, false, false);
                            continue;
                        }

                        // A solution was found with yaw AND pitch, so just immediately return it.
                        return new Solution(context, new Rotation(yaw, pitch.left()), dest, true, pitch.right());
                    }
                }
            }
        }
        return solution;
    }

    private void tickUseFireworks(final Vector3d start, final Vector3d goingTo, final boolean isBoosted, final boolean forceUseFirework) {
        if (this.remainingSetBackTicks > 0) {
            debug("waiting for elytraFireworkSetbackUseDelay: " + this.remainingSetBackTicks);
            return;
        }
        if (this.landingMode) {
            return;
        }
        final boolean useOnDescend = !CONFIG.client.extra.pathfinder.elytra.conserveFireworks || ctx.playerFeetAsVec().getY() < goingTo.getY() + 5;
        final double currentSpeed = Vector3d.from(
                ctx.playerMotion().getX(),
                // ignore y component if we are BOTH below where we want to be AND descending
                ctx.playerFeetAsVec().getY() < goingTo.getY() ? Math.max(0, ctx.playerMotion().getY()) : ctx.playerMotion().getY(),
                ctx.playerMotion().getZ()
        ).lengthSquared();

        final double elytraFireworkSpeed = CONFIG.client.extra.pathfinder.elytra.fireworkSpeed;
        if (this.remainingFireworkTicks <= 0 && (forceUseFirework || (!isBoosted
                && useOnDescend
                && (ctx.playerFeetAsVec().getY() < goingTo.getY() - 5 || start.distance(Vector3d.from(goingTo.getX() + 0.5, ctx.playerFeetAsVec().getY(), goingTo.getZ() + 0.5)) > 5) // UGH!!!!!!!
                && currentSpeed < elytraFireworkSpeed * elytraFireworkSpeed))
        ) {
            // Prioritize boosting fireworks over regular ones
            // TODO: Take the minimum boost time into account?
            if (!isFireworks(CACHE.getPlayerCache().getEquipment(EquipmentSlot.MAIN_HAND))
                && !isFireworks(CACHE.getPlayerCache().getEquipment(EquipmentSlot.OFF_HAND))
                && !baritone.getInventoryBehavior().throwaway(true, ElytraBehavior::isBoostingFireworks) &&
                    !baritone.getInventoryBehavior().throwaway(true, ElytraBehavior::isFireworks)) {
                info("no fireworks");
                return;
            }
            logVerbose("attempting to use firework" + (forceUseFirework ? " (forced)" : ""));
            var mainHand = CACHE.getPlayerCache().getEquipment(EquipmentSlot.MAIN_HAND);
            var offHand = CACHE.getPlayerCache().getEquipment(EquipmentSlot.OFF_HAND);
            // InventoryBehavior may only have queued a hotbar move. Wait until a rocket is held.
            if (!isFireworks(mainHand) && !isFireworks(offHand)) return;
            this.fireworkHand = isFireworks(mainHand) ? Hand.MAIN_HAND : Hand.OFF_HAND;
            this.useFirework = true;
            this.requestedBoostTicks = 10 * (1 + getFireworkBoost((fireworkHand == Hand.MAIN_HAND ? mainHand : offHand)).orElse(0));

        }
    }

    public void onFireworkUsed() {
        if (destroyed || !useFirework) return;
        this.minimumBoostTicks = requestedBoostTicks;
        this.remainingFireworkTicks = 10;
        this.deployedFireworkLastTick = true;
    }

    private final class SolverContext {

        public final NetherPath path;
        public final int playerNear;
        public final Vector3d start;
        public final Vector3d motion;
        public final LocalizedCollisionBox boundingBox;
        public final boolean ignoreLava;
        public final FireworkBoost boost;

        /**
         * Creates a new SolverContext using the current state of the path, player, and firework boost at the time of
         * construction.
         *
         * @param async Whether the computation is being done asynchronously at the end of a game tick.
         */
        public SolverContext(boolean async) {
            this.path = ElytraBehavior.this.pathManager.getPath();
            this.playerNear = ElytraBehavior.this.pathManager.getNear();

            this.start = ctx.playerFeetAsVec();
            this.motion = ctx.playerMotion();
            this.boundingBox = ctx.player().getPlayerCollisionBox();
            this.ignoreLava = ctx.player().isTouchingLava();

            final Integer fireworkTicksExisted;
            if (async && ElytraBehavior.this.deployedFireworkLastTick) {
                final int[] counter = ElytraBehavior.this.nextTickBoostCounter;
                fireworkTicksExisted = counter[1] > counter[0] ? 0 : null;
            } else {
                fireworkTicksExisted = ElytraBehavior.this.getAttachedFirework().map(Entity::getTickCount).orElse(null);
            }
            this.boost = new FireworkBoost(fireworkTicksExisted, ElytraBehavior.this.minimumBoostTicks);

            // ZenithProxy applies requested rotations directly, without a client aim processor.
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || o.getClass() != SolverContext.class) {
                return false;
            }

            var other = (SolverContext) o;
            return this.path == other.path  // Contents aren't modified, just compare by reference
                    && this.playerNear == other.playerNear
                    && Objects.equals(this.start, other.start)
                    && Objects.equals(this.motion, other.motion)
                    && Objects.equals(this.boundingBox, other.boundingBox)
                    && this.ignoreLava == other.ignoreLava
                    && Objects.equals(this.boost, other.boost);
        }
    }

    private static final class FireworkBoost {

        private final Integer fireworkTicksExisted;
        private final int minimumBoostTicks;
        private final int maximumBoostTicks;

        /**
         * @param fireworkTicksExisted The ticksExisted of the attached firework entity, or {@code null} if no entity.
         * @param minimumBoostTicks    The minimum number of boost ticks that the attached firework entity, if any, will
         *                             provide.
         */
        public FireworkBoost(final Integer fireworkTicksExisted, final int minimumBoostTicks) {
            this.fireworkTicksExisted = fireworkTicksExisted;

            // this.lifetime = 10 * i + this.rand.nextInt(6) + this.rand.nextInt(7);
            this.minimumBoostTicks = minimumBoostTicks;
            this.maximumBoostTicks = minimumBoostTicks + 11;
        }

        public boolean isBoosted() {
            return this.fireworkTicksExisted != null;
        }

        /**
         * @return The guaranteed number of remaining ticks with boost
         */
        public int getGuaranteedBoostTicks() {
            return this.isBoosted() ? Math.max(0, this.minimumBoostTicks - this.fireworkTicksExisted) : 0;
        }

        /**
         * @return The maximum number of remaining ticks with boost
         */
        public int getMaximumBoostTicks() {
            return this.isBoosted() ? Math.max(0, this.maximumBoostTicks - this.fireworkTicksExisted) : 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || o.getClass() != FireworkBoost.class) {
                return false;
            }

            var other = (FireworkBoost) o;
            if (!this.isBoosted() && !other.isBoosted()) {
                return true;
            }

            return Objects.equals(this.fireworkTicksExisted, other.fireworkTicksExisted)
                    && this.minimumBoostTicks == other.minimumBoostTicks
                    && this.maximumBoostTicks == other.maximumBoostTicks;
        }
    }

    private static final class PitchResult {

        public final float pitch;
        public final double dot;
        public final List<Vector3d> steps;

        public PitchResult(float pitch, double dot, List<Vector3d> steps) {
            this.pitch = pitch;
            this.dot = dot;
            this.steps = steps;
        }
    }

    private static final class Solution {

        public final SolverContext context;
        public final Rotation rotation;
        public final Vector3d goingTo;
        public final boolean solvedPitch;
        public final boolean forceUseFirework;

        public Solution(SolverContext context, Rotation rotation, Vector3d goingTo, boolean solvedPitch, boolean forceUseFirework) {
            this.context = context;
            this.rotation = rotation;
            this.goingTo = goingTo;
            this.solvedPitch = solvedPitch;
            this.forceUseFirework = forceUseFirework;
        }
    }

    public static boolean isFireworks(final ItemStack itemStack) {
        return itemStack != null && itemStack.getId() == ItemRegistry.FIREWORK_ROCKET.id()
            && getFireworkBoost(itemStack).isPresent();
    }

    private static boolean isBoostingFireworks(final ItemStack itemStack) {
        return isFireworks(itemStack);
    }

    private static OptionalInt getFireworkBoost(final ItemStack itemStack) {
        if (itemStack == null || itemStack.getId() != ItemRegistry.FIREWORK_ROCKET.id()) return OptionalInt.empty();
        var fw = itemStack.getDataComponentsOrEmpty().getOrDefault(DataComponentTypes.FIREWORKS,
            ItemRegistry.FIREWORK_ROCKET.components().get(DataComponentTypes.FIREWORKS));
        if (fw != null && fw.getExplosions().isEmpty()) {
            return OptionalInt.of(fw.getFlightDuration());
        }
        return OptionalInt.empty();
    }

    private Optional<Entity> getAttachedFirework() {
        return CACHE.getEntityCache().getEntities().values().stream()
            .filter(e -> !e.isRemoved() && e.getEntityType() == EntityType.FIREWORK_ROCKET)
            .filter(e -> {
                var attached = e.getMetadataValue(9, MetadataTypes.OPTIONAL_VARINT, OptionalInt.class);
                return attached != null && attached.isPresent() && attached.getAsInt() == CACHE.getPlayerCache().getEntityId();
            })
            .findFirst();
    }

    private boolean isHitboxClear(final SolverContext context, final Vector3d dest, final Double growAmount) {
        var start = context.start;
        final boolean ignoreLava = context.ignoreLava;

        if (!this.clearView(start, dest, ignoreLava)) {
            return false;
        }
        if (growAmount == null) {
            return true;
        }

        var bb = context.boundingBox.inflate(growAmount, growAmount, growAmount);

        final double ox = dest.getX() - start.getX();
        final double oy = dest.getY() - start.getY();
        final double oz = dest.getZ() - start.getZ();

        final double[] src = new double[]{
                bb.minX(), bb.minY(), bb.minZ(),
                bb.minX(), bb.minY(), bb.maxZ(),
                bb.minX(), bb.maxY(), bb.minZ(),
                bb.minX(), bb.maxY(), bb.maxZ(),
                bb.maxX(), bb.minY(), bb.minZ(),
                bb.maxX(), bb.minY(), bb.maxZ(),
                bb.maxX(), bb.maxY(), bb.minZ(),
                bb.maxX(), bb.maxY(), bb.maxZ(),
        };
        final double[] dst = new double[]{
                bb.minX() + ox, bb.minY() + oy, bb.minZ() + oz,
                bb.minX() + ox, bb.minY() + oy, bb.maxZ() + oz,
                bb.minX() + ox, bb.maxY() + oy, bb.minZ() + oz,
                bb.minX() + ox, bb.maxY() + oy, bb.maxZ() + oz,
                bb.maxX() + ox, bb.minY() + oy, bb.minZ() + oz,
                bb.maxX() + ox, bb.minY() + oy, bb.maxZ() + oz,
                bb.maxX() + ox, bb.maxY() + oy, bb.minZ() + oz,
                bb.maxX() + ox, bb.maxY() + oy, bb.maxZ() + oz,
        };

        // Use non-batching method without early failure
        if (false /* Baritone.settings().elytraRenderHitboxRaytraces */) {
            boolean clear = true;
            for (int i = 0; i < 8; i++) {
                var s = Vector3d.from(src[i * 3], src[i * 3 + 1], src[i * 3 + 2]);
                var d = Vector3d.from(dst[i * 3], dst[i * 3 + 1], dst[i * 3 + 2]);
                // Don't forward ignoreLava since the batch call doesn't care about it
                if (!this.clearView(s, d, false)) {
                    clear = false;
                }
            }
            return clear;
        }


        return raytrace(8, src, dst, NetherPathfinderContext.Visibility.ALL);
    }

    public boolean clearView(Vector3d start, Vector3d dest, boolean ignoreLava) {
        final boolean clear;
        if (!ignoreLava) {
            // if start == dest then the cpp raytracer dies
            clear = start.equals(dest) || raytrace(start, dest);
        } else {
            clear = clearWorldView(start, dest);
        }

        if (false /* Baritone.settings().elytraRenderRaytraces */) {
            (clear ? this.clearLines : this.blockedLines).add(new Pair<>(start, dest));
        }
        return clear;
    }

    private static FloatArrayList pitchesToSolveFor(final float goodPitch, final boolean desperate) {
        final float minPitch = desperate ? -90 : Math.max(goodPitch - 25 /* Baritone.settings().elytraPitchRange */, -89);
        final float maxPitch = desperate ? 90 : Math.min(goodPitch + 25 /* Baritone.settings().elytraPitchRange */, 89);

        var pitchValues = new FloatArrayList(ceilI(maxPitch - minPitch) + 1);
        for (float pitch = goodPitch; pitch <= maxPitch; pitch++) {
            pitchValues.add(pitch);
        }
        for (float pitch = goodPitch - 1; pitch >= minPitch; pitch--) {
            pitchValues.add(pitch);
        }

        return pitchValues;
    }

    @FunctionalInterface
    private interface IntTriFunction<T> {
        T apply(int first, int second, int third);
    }

    private static final class IntTriple {
        public final int first;
        public final int second;
        public final int third;

        public IntTriple(int first, int second, int third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }
    }

    private Pair<Float, Boolean> solvePitch(final SolverContext context, final Vector3d goal, final int relaxation) {
        final boolean desperate = relaxation == 2;
        final float goodPitch = RotationUtils.calcRotationFromVec3d(context.start, goal, ctx.playerRotations()).pitch();
        var pitches = pitchesToSolveFor(goodPitch, desperate);

        final IntTriFunction<PitchResult> solve = (ticks, ticksBoosted, ticksBoostDelay) ->
                this.solvePitch(context, goal, relaxation, pitches.iterator(), ticks, ticksBoosted, ticksBoostDelay);

        final List<IntTriple> tests = new ArrayList<>();

        if (context.boost.isBoosted()) {
            final int guaranteed = context.boost.getGuaranteedBoostTicks();
            if (guaranteed == 0) {
                // uncertain when boost will run out
                final int lookahead = Math.max(4, 10 - context.boost.getMaximumBoostTicks());
                tests.add(new IntTriple(lookahead, 1, 0));
            } else if (guaranteed <= 5) {
                // boost will run out within 5 ticks
                tests.add(new IntTriple(guaranteed + 5, guaranteed, 0));
            } else {
                // there's plenty of guaranteed boost
                tests.add(new IntTriple(guaranteed + 1, guaranteed, 0));
            }
        }

        // Standard test, assume (not) boosted for entire duration
        final int ticks = desperate ? 3 : context.boost.isBoosted() ? Math.max(5, context.boost.getGuaranteedBoostTicks()) : 20 /* Baritone.settings().elytraSimulationTicks */;
        tests.add(new IntTriple(ticks, context.boost.isBoosted() ? ticks : 0, 0));

        final Optional<PitchResult> result = tests.stream()
                .map(i -> solve.apply(i.first, i.second, i.third))
                .filter(Objects::nonNull)
                .findFirst();
        if (result.isPresent()) {
            return new Pair<>(result.get().pitch, false);
        }

        // If we used a firework would we be able to get out of the current situation??? perhaps
        if (desperate) {
            final List<IntTriple> testsBoost = new ArrayList<>();
            testsBoost.add(new IntTriple(ticks, 10, 3));
            testsBoost.add(new IntTriple(ticks, 10, 2));
            testsBoost.add(new IntTriple(ticks, 10, 1));

            final Optional<PitchResult> resultBoost = testsBoost.stream()
                    .map(i -> solve.apply(i.first, i.second, i.third))
                    .filter(Objects::nonNull)
                    .findFirst();
            if (resultBoost.isPresent()) {
                return new Pair<>(resultBoost.get().pitch, true);
            }
        }

        return null;
    }

    private PitchResult solvePitch(final SolverContext context, final Vector3d goal, final int relaxation,
                                   final FloatIterator pitches, final int ticks, final int ticksBoosted,
                                   final int ticksBoostDelay) {
        // we are at a certain velocity, but we have a target velocity
        // what pitch would get us closest to our target velocity?
        // yaw is easy so we only care about pitch

        var goalDelta = goal.sub(context.start);
        var goalDirection = goalDelta.lengthSquared() < 1.0E-8 ? Vector3d.ZERO : goalDelta.normalize();

        final Deque<PitchResult> bestResults = new ArrayDeque<>();

        while (pitches.hasNext()) {
            final float pitch = pitches.nextFloat();
            final List<Vector3d> displacement = this.simulate(
                    context,
                    goalDelta,
                    pitch,
                    ticks,
                    ticksBoosted,
                    ticksBoostDelay
            );
            if (displacement == null) {
                continue;
            }
            var last = displacement.get(displacement.size() - 1);
            double goodness = goalDirection.dot(last.lengthSquared() < 1.0E-8 ? Vector3d.ZERO : last.normalize());
            if (landingMode) {
                goodness = -goalDelta.sub(last).length();
            }
            var bestSoFar = bestResults.peek();
            if (bestSoFar == null || goodness > bestSoFar.dot) {
                bestResults.push(new PitchResult(pitch, goodness, displacement));
            }
        }

        outer:
        for (final PitchResult result : bestResults) {
            if (relaxation < 2) {
                // Ensure that the goal is visible along the entire simulated path
                // Reverse order iteration since the last position is most likely to fail
                for (int i = result.steps.size() - 1; i >= 1; i--) {
                    if (!clearView(context.start.add(result.steps.get(i)), goal, context.ignoreLava)) {
                        continue outer;
                    }
                }
            } else {
                // Ensure that the goal is visible from the final position
                if (!clearView(context.start.add(result.steps.get(result.steps.size() - 1)), goal, context.ignoreLava)) {
                    continue;
                }
            }

            this.simulationLine = result.steps;
            return result;
        }
        return null;
    }

    private List<Vector3d> simulate(final SolverContext context, final Vector3d goalDelta, final float pitch, final int ticks,
                                final int ticksBoosted, final int ticksBoostDelay) {
        var delta = goalDelta;
        var motion = context.motion;
        var hitbox = context.boundingBox;
        List<Vector3d> displacement = new ArrayList<>(ticks + 1);
        displacement.add(Vector3d.ZERO);
        int remainingTicksBoosted = ticksBoosted;

        for (int i = 0; i < ticks; i++) {
            if (delta.lengthSquared() < 1) {
                break;
            }
            var rotation = RotationUtils.calcRotationFromVec3d(Vector3d.ZERO, delta, ctx.playerRotations()).withPitch(pitch);
            var lookDirection = RotationUtils.calcLookDirectionFromRotation(rotation);

            if (Thread.currentThread().isInterrupted()) return null;
            motion = step(motion, lookDirection, rotation.pitch());
            delta = delta.sub(motion);

            // Collision box while the player is in motion, with additional padding for safety
            // Use expandTowards for directional swept volume (fixes #5049)
            // expandTowards handles negative vectors correctly (unlike inflate)
            // and provides full swept volume coverage (unlike move)
            var inMotion = hitbox.stretch(motion.getX(), motion.getY(), motion.getZ()).inflate(0.01, 0.01, 0.01);

            int xmin = floorI(inMotion.minX());
            int xmax = ceilI(inMotion.maxX());
            int ymin = floorI(inMotion.minY());
            int ymax = ceilI(inMotion.maxY());
            int zmin = floorI(inMotion.minZ());
            int zmax = ceilI(inMotion.maxZ());
            for (int x = xmin; x < xmax; x++) {
                for (int y = ymin; y < ymax; y++) {
                    for (int z = zmin; z < zmax; z++) {
                        if (!this.passable(x, y, z, context.ignoreLava)) {
                            return null;
                        }
                    }
                }
            }

            hitbox = hitbox.move(motion.getX(), motion.getY(), motion.getZ());
            displacement.add(displacement.get(displacement.size() - 1).add(motion));

            if (i >= ticksBoostDelay && remainingTicksBoosted-- > 0) {
                // See EntityFireworkRocket
                motion = motion.add(
                        lookDirection.getX() * 0.1 + (lookDirection.getX() * 1.5 - motion.getX()) * 0.5,
                        lookDirection.getY() * 0.1 + (lookDirection.getY() * 1.5 - motion.getY()) * 0.5,
                        lookDirection.getZ() * 0.1 + (lookDirection.getZ() * 1.5 - motion.getZ()) * 0.5
                );
            }
        }

        return displacement;
    }

    private static Vector3d step(final Vector3d motion, final Vector3d lookDirection, final float pitch) {
        double motionX = motion.getX();
        double motionY = motion.getY();
        double motionZ = motion.getZ();

        float pitchRadians = pitch * RotationUtils.DEG_TO_RAD_F;
        double pitchBase2 = Math.sqrt(lookDirection.getX() * lookDirection.getX() + lookDirection.getZ() * lookDirection.getZ());
        double flatMotion = Math.sqrt(motionX * motionX + motionZ * motionZ);
        double thisIsAlwaysOne = lookDirection.length();
        float pitchBase3 = (float) Math.cos(pitchRadians);
        //System.out.println("always the same lol " + -pitchBase + " " + pitchBase3);
        //System.out.println("always the same lol " + Math.abs(pitchBase3) + " " + pitchBase2);
        //System.out.println("always 1 lol " + thisIsAlwaysOne);
        pitchBase3 = (float) ((double) pitchBase3 * (double) pitchBase3 * Math.min(1, thisIsAlwaysOne / 0.4));
        motionY += -0.08 + (double) pitchBase3 * 0.06;
        if (motionY < 0 && pitchBase2 > 0) {
            double speedModifier = motionY * -0.1 * (double) pitchBase3;
            motionY += speedModifier;
            motionX += lookDirection.getX() * speedModifier / pitchBase2;
            motionZ += lookDirection.getZ() * speedModifier / pitchBase2;
        }
        if (pitchRadians < 0 && pitchBase2 > 0) { // if you are looking down (below level)
            double anotherSpeedModifier = flatMotion * (double) (-(float) Math.sin(pitchRadians)) * 0.04;
            motionY += anotherSpeedModifier * 3.2;
            motionX -= lookDirection.getX() * anotherSpeedModifier / pitchBase2;
            motionZ -= lookDirection.getZ() * anotherSpeedModifier / pitchBase2;
        }
        if (pitchBase2 > 0) { // this is always true unless you are looking literally straight up (let's just say the bot will never do that)
            motionX += (lookDirection.getX() / pitchBase2 * flatMotion - motionX) * 0.1;
            motionZ += (lookDirection.getZ() / pitchBase2 * flatMotion - motionZ) * 0.1;
        }
        motionX *= 0.99f;
        motionY *= 0.98f;
        motionZ *= 0.99f;
        //System.out.println(motionX + " " + motionY + " " + motionZ);

        return Vector3d.from(motionX, motionY, motionZ);
    }

    // any call to this must be done with the lock held
    private boolean passable(int x, int y, int z, boolean ignoreLava) {
        if (ignoreLava) {
            if (!World.isChunkLoadedBlockPos(x, z)) return false;
            var block = World.getBlock(x, y, z);
            return block.isAir() || block == BlockRegistry.LAVA;
        } else {
            return passable(x, y, z);
        }
    }

    private void tickInventoryTransactions() {
        if (inventoryRequest != null && inventoryRequest.isCompleted()) inventoryRequest = null;
    }

    private int findGoodElytra() {
        var invy = CACHE.getPlayerCache().getPlayerInventory();
        for (int i = 9; i < 45; i++) {
            var slot = invy.get(i);
            if (slot != null && slot.getId() == ItemRegistry.ELYTRA.id()
                && ItemUtil.getDamageUntilBreak(slot) > CONFIG.client.extra.pathfinder.elytra.minimumDurability) {
                return i;
            }
        }
        return -1;
    }

    private void trySwapElytra() {
        if (!CONFIG.client.extra.pathfinder.elytra.autoSwap || inventoryRequest != null
            || CACHE.getPlayerCache().getInventoryCache().getOpenContainerId() != 0) return;
        var chest = CACHE.getPlayerCache().getEquipment(EquipmentSlot.CHESTPLATE);
        if (chest == null || chest.getId() != ItemRegistry.ELYTRA.id()
            || ItemUtil.getDamageUntilBreak(chest) > CONFIG.client.extra.pathfinder.elytra.minimumDurability) return;
        int goodElytraSlot = findGoodElytra();
        if (goodElytraSlot != -1) {
            inventoryRequest = INVENTORY.submit(InventoryActionRequest.builder()
                .owner(this)
                .priority(Baritone.getPriority())
                .actionDelayTicks(1) // Baritone.settings().ticksBetweenInventoryMoves
                .actions(new ClickItem(goodElytraSlot, ClickItemAction.LEFT_CLICK),
                    new ClickItem(6, ClickItemAction.LEFT_CLICK),
                    new ClickItem(goodElytraSlot, ClickItemAction.LEFT_CLICK))
                .build());
        }
    }

    void logVerbose(String message) {
        if (CONFIG.client.extra.pathfinder.elytra.verboseLogging) {
            debug(message);
        }
    }

    // so we don't get stuck trying to pathfind through the roof
    private BlockPos fixDestination(BlockPos dst) {
        if (World.getCurrentDimension() == DimensionRegistry.THE_NETHER.get()) {
            if (ctx.player().getY() >= 128 && dst.y() < 128) {
                return new BlockPos(dst.x(), 128, dst.z());
            }
            else if (ctx.player().getY() < 128 && dst.y() >= 128) {
                return new BlockPos(dst.x(), 64, dst.z());
            }
        }
        return dst;
    }

    private BlockPos destinationFixed() {
        return fixDestination(this.destination);
    }

    private boolean clearWorldView(Vector3d start, Vector3d end) {
        // World returns air for unloaded chunks; do not let a collision check cross one.
        int steps = Math.max(1, (int) Math.ceil(start.distance(end) * 2));
        for (int i = 0; i <= steps; i++) {
            var point = start.add(end.sub(start).mul((double) i / steps));
            if (!World.isChunkLoadedBlockPos(floorI(point.getX()), floorI(point.getZ()))) return false;
        }
        return !RaycastHelper.blockRaycast(start.getX(), start.getY(), start.getZ(),
            end.getX(), end.getY(), end.getZ(), false).hit();
    }

    public boolean raytrace(double startX, double startY, double startZ, double endX, double endY, double endZ) {
        final int maxHeight = npfContext.getMaxHeight() + World.getCurrentDimension().minY();
        final int minHeight = World.getCurrentDimension().minY();
        final boolean isOOB = startY >= maxHeight || endY >= maxHeight || startY < minHeight || endY < minHeight;
        if (isOOB) {
            var start = Vector3d.from(startX, startY, startZ);
            var end = Vector3d.from(endX, endY, endZ);
            return clearWorldView(start, end);
        }

        return npfContext.raytrace(startX, startY, startZ, endX, endY, endZ);
    }

    public boolean raytrace(Vector3d start, Vector3d end) {
        final int maxHeight = npfContext.getMaxHeight() + World.getCurrentDimension().minY();
        final int minHeight = World.getCurrentDimension().minY();
        final boolean isOOB = start.getY() >= maxHeight || end.getY() >= maxHeight || start.getY() < minHeight || end.getY() < minHeight;
        if (isOOB) {
            return clearWorldView(start, end);
        }
        return npfContext.raytrace(start.getX(), start.getY(), start.getZ(), end.getX(), end.getY(), end.getZ());
    }

    public boolean raytrace(int count, double[] src, double[] dst, int visibility) {
        if (src.length != count * 3 || src.length != dst.length) {
            throw new IllegalArgumentException("Expected source and dst to have length of " + (count * 3));
        }
        final int maxHeight = npfContext.getMaxHeight() + World.getCurrentDimension().minY();

        boolean isOOB = false;
        for(int i = 1; i < src.length; i += 3) {
            if (src[i] >= maxHeight || src[i] < World.getCurrentDimension().minY() ||
                    dst[i] >= maxHeight || dst[i] < World.getCurrentDimension().minY()) {
                isOOB = true;
                break;
            }
        }

        if (isOOB) {
            for (int i = 0; i < count; i++) {
                var start = Vector3d.from(src[i * 3], src[i * 3 + 1], src[i * 3 + 2]);
                var end = Vector3d.from(dst[i * 3], dst[i * 3 + 1], dst[i * 3 + 2]);
                boolean clear = clearWorldView(start, end);
                switch (visibility) {
                    case NetherPathfinderContext.Visibility.ALL -> { if (!clear) return false; }
                    case NetherPathfinderContext.Visibility.NONE -> { if (clear) return false; }
                    case NetherPathfinderContext.Visibility.ANY -> { if (clear) return true; }
                    default -> throw new IllegalArgumentException("Unknown visibility mode: " + visibility);
                }
            }
            return visibility != NetherPathfinderContext.Visibility.ANY;
        }

        return npfContext.raytrace(count, src, dst, visibility);
    }

    public boolean passable(int x, int y, int z) {
        if (y < World.getCurrentDimension().minY()) return false;
        if (y >= World.getCurrentDimension().buildHeight()) return World.isChunkLoadedBlockPos(x, z);
        return npfContext.passable(x, y, z);
    }
}
