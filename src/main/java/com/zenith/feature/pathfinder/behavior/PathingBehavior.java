package com.zenith.feature.pathfinder.behavior;

import com.google.common.util.concurrent.ListenableFuture;
import com.zenith.feature.pathfinder.Baritone;
import com.zenith.feature.pathfinder.BlockStateInterface;
import com.zenith.feature.pathfinder.PathingCommand;
import com.zenith.feature.pathfinder.calc.AStarPathFinder;
import com.zenith.feature.pathfinder.calc.AbstractNodeCostSearch;
import com.zenith.feature.pathfinder.calc.IPath;
import com.zenith.feature.pathfinder.executor.PathExecutor;
import com.zenith.feature.pathfinder.goals.Goal;
import com.zenith.feature.pathfinder.goals.GoalXZ;
import com.zenith.feature.pathfinder.goals.PosGoal;
import com.zenith.feature.pathfinder.movement.CalculationContext;
import com.zenith.feature.pathfinder.movement.MovementHelper;
import com.zenith.feature.pathfinder.util.Favoring;
import com.zenith.feature.pathfinder.util.PathCalculationResult;
import com.zenith.feature.pathfinder.util.PathEvent;
import com.zenith.mc.block.BlockPos;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.zenith.Globals.*;
import static java.util.Objects.requireNonNullElse;

@Getter
public class PathingBehavior extends Behavior {
    private PathExecutor current;
    private PathExecutor next;
    private Goal goal;
    private CalculationContext context;
    /*eta*/
    private int ticksElapsedSoFar;
    private BlockPos startPosition;
    private boolean safeToCancel;
    private boolean pauseRequestedLastTick;
    private boolean unpausedLastTick;
    private boolean pausedThisTick;
    private boolean cancelRequested;
    private boolean calcFailedLastTick;
    private volatile AbstractNodeCostSearch inProgress;
    private final Object pathCalcLock = new Object();
    private final Object pathPlanLock = new Object();
    @Nullable private volatile ListenableFuture<?> pathSearchFuture;
    private boolean lastAutoJump;
    private BlockPos expectedSegmentStart;
    private final LinkedBlockingQueue<PathEvent> toDispatch = new LinkedBlockingQueue<>();

    public PathingBehavior(Baritone baritone) {
        super(baritone);
    }

    private void queuePathEvent(PathEvent event) {
        toDispatch.add(event);
    }

    private void dispatchEvents() {
        ArrayList<PathEvent> curr = new ArrayList<>();
        toDispatch.drainTo(curr);
        calcFailedLastTick = curr.contains(PathEvent.CALC_FAILED);
//        for (PathEvent event : curr) {
//            baritone.getGameEventHandler().onPathEvent(event);
//        }
    }

    public void onClientBotTickStateChange() {
        secretInternalSegmentCancel();
        baritone.getPathingControlManager().cancelEverything();
    }

    public void onTick() {
        dispatchEvents();

        expectedSegmentStart = pathStart();
        baritone.getPathingControlManager().preTick();
        tickPath();
        ticksElapsedSoFar++;
        dispatchEvents();
    }

    private void tickPath() {
        pausedThisTick = false;
        if (pauseRequestedLastTick && safeToCancel) {
            pauseRequestedLastTick = false;
            if (unpausedLastTick) {
                baritone.getInputOverrideHandler().clearAllKeys();
            }
            unpausedLastTick = false;
            pausedThisTick = true;
            return;
        }
        unpausedLastTick = true;
        if (cancelRequested) {
            cancelRequested = false;
            baritone.getInputOverrideHandler().clearAllKeys();
        }
        synchronized (pathPlanLock) {
            synchronized (pathCalcLock) {
                if (inProgress != null) {
                    // we are calculating
                    // are we calculating the right thing though? 🤔
                    BlockPos calcFrom = inProgress.getStart();
                    Optional<IPath> currentBest = inProgress.bestPathSoFar();
                    if ((current == null || !current.getPath().getDest().equals(calcFrom)) // if current ends in inProgress's start, then we're ok
                        && !calcFrom.equals(ctx.playerFeet()) && !calcFrom.equals(expectedSegmentStart) // if current starts in our playerFeet or pathStart, then we're ok
                        && (!currentBest.isPresent() || (!currentBest.get().positions().contains(ctx.playerFeet()) && !currentBest.get().positions().contains(expectedSegmentStart))) // if
                    ) {
                        // when it was *just* started, currentBest will be empty so we need to also check calcFrom since that's always present
                        inProgress.cancel(); // cancellation doesn't dispatch any events
                    }
                }
            }
            if (current == null) {
                return;
            }
            safeToCancel = current.onTick();
            if (System.currentTimeMillis() - lastFailedPathTime < 3000) {
                return;
            }
            if (current.failed() || current.finished()) {
                current = null;
                if (goal == null || goal.isInGoal(ctx.playerFeet())) {
                    PATH_LOG.info("All done. At {}", requireNonNullElse(goal, "goal"));
                    queuePathEvent(PathEvent.AT_GOAL);
                    next = null;
//                    if (Baritone.settings().disconnectOnArrival.value) {
//                        ctx.world().disconnect();
//                    }
                    return;
                }
                if (next != null && !next.getPath().positions().contains(ctx.playerFeet()) && !next.getPath().positions().contains(expectedSegmentStart)) { // can contain either one
                    // if the current path failed, we may not actually be on the next one, so make sure
                    PATH_LOG.debug("Discarding next path as it does not contain current position");
                    // for example if we had a nicely planned ahead path that starts where current ends
                    // that's all fine and good
                    // but if we fail in the middle of current
                    // we're nowhere close to our planned ahead path
                    // so need to discard it sadly.
                    queuePathEvent(PathEvent.DISCARD_NEXT);
                    next = null;
                }
                if (next != null) {
                    PATH_LOG.debug("Continuing on to planned next path");
                    queuePathEvent(PathEvent.CONTINUING_ONTO_PLANNED_NEXT);
                    current = next;
                    next = null;
                    current.onTick(); // don't waste a tick doing nothing, get started right away
                    return;
                }
                // at this point, current just ended, but we aren't in the goal and have no plan for the future
                synchronized (pathCalcLock) {
                    ListenableFuture<?> searchFuture = pathSearchFuture;
                    if (inProgress != null || (searchFuture != null && !searchFuture.isDone())) {
                        queuePathEvent(PathEvent.PATH_FINISHED_NEXT_STILL_CALCULATING);
                        return;
                    }
                    // we aren't calculating
                    queuePathEvent(PathEvent.CALC_STARTED);
                    findPathInNewThread(expectedSegmentStart, context);
                }
                return;
            }
            // at this point, we know current is in progress
            if (safeToCancel && next != null && next.snipsnapifpossible()) {
                // a movement just ended; jump directly onto the next path
                PATH_LOG.debug("Splicing into planned next path early...");
                queuePathEvent(PathEvent.SPLICING_ONTO_NEXT_EARLY);
                current = next;
                next = null;
                current.onTick();
                return;
            }
            current = current.trySplice(next);
            if (next != null && current.getPath().getDest().equals(next.getPath().getDest())) {
                next = null;
            }
            synchronized (pathCalcLock) {
                ListenableFuture<?> searchFuture = pathSearchFuture;
                if (inProgress != null || (searchFuture != null && !searchFuture.isDone())) {
                    // if we aren't calculating right now
                    return;
                }
                if (next != null) {
                    // and we have no plan for what to do next
                    return;
                }
                if (goal == null || goal.isInGoal(current.getPath().getDest())) {
                    // and this path doesn't get us all the way there
                    return;
                }
                if (ticksRemainingInSegment(false).get() < 150) {
                    // and this path has 7.5 seconds or less left
                    // don't include the current movement so a very long last movement (e.g. descend) doesn't trip it up
                    // if we actually included current, it wouldn't start planning ahead until the last movement was done, if the last movement took more than 7.5 seconds on its own
                    PATH_LOG.debug("Path almost over. Planning ahead...");
                    queuePathEvent(PathEvent.NEXT_SEGMENT_CALC_STARTED);
                    findPathInNewThread(current.getPath().getDest(), context);
                }
            }
        }
    }

    public void secretInternalSetGoal(Goal goal) {
        this.goal = goal;
    }

    public boolean secretInternalSetGoalAndPath(PathingCommand command) {
        secretInternalSetGoal(command.goal);
        context = new CalculationContext(command.goal);
        if (goal == null) {
            return false;
        }
        if (goal.isInGoal(ctx.playerFeet())) {
            return false;
        }
        synchronized (pathPlanLock) {
            if (current != null) {
                return false;
            }
            synchronized (pathCalcLock) {
                ListenableFuture<?> searchFuture = pathSearchFuture;
                if (inProgress != null || (searchFuture != null && !searchFuture.isDone())) {
                    return false;
                }
                queuePathEvent(PathEvent.CALC_STARTED);
                findPathInNewThread(expectedSegmentStart, context);
                return true;
            }
        }
    }

    public boolean isPathing() {
        return hasPath() && !pausedThisTick;
    }

    public Optional<AbstractNodeCostSearch> getInProgress() {
        return Optional.ofNullable(inProgress);
    }

    public boolean isSafeToCancel() {
        if (current == null) {
            return true;
        }
        return safeToCancel;
    }

    public void requestPause() {
        pauseRequestedLastTick = true;
    }

    public boolean cancelSegmentIfSafe() {
        if (isSafeToCancel()) {
            secretInternalSegmentCancel();
            return true;
        }
        return false;
    }

    public boolean cancelEverything() {
        boolean doIt = isSafeToCancel();
        if (doIt) {
            secretInternalSegmentCancel();
        }
        baritone.getPathingControlManager().cancelEverything(); // regardless of if we can stop the current segment, we can still stop the processes
        return doIt;
    }

    public boolean calcFailedLastTick() { // NOT exposed on public api
        return calcFailedLastTick;
    }

    public void softCancelIfSafe() {
        synchronized (pathPlanLock) {
            getInProgress().ifPresent(AbstractNodeCostSearch::cancel); // only cancel ours
            if (!isSafeToCancel()) {
                return;
            }
            current = null;
            next = null;
        }
        cancelRequested = true;
        // do everything BUT clear keys
    }

    // just cancel the current path
    public void secretInternalSegmentCancel() {
        queuePathEvent(PathEvent.CANCELED);
        synchronized (pathPlanLock) {
            getInProgress().ifPresent(AbstractNodeCostSearch::cancel);
            if (current != null) {
                current = null;
                next = null;
                baritone.getInputOverrideHandler().clearAllKeys();
            }
        }
    }

    public void forceCancel() { // exposed on public api because :sob:
        cancelEverything();
        secretInternalSegmentCancel();
        synchronized (pathCalcLock) {
            inProgress = null;
        }
    }

    public CalculationContext secretInternalGetCalculationContext() {
        return context;
    }

    public Optional<Double> estimatedTicksToGoal() {
        BlockPos currentPos = ctx.playerFeet();
        if (goal == null || currentPos == null || startPosition == null) {
            return Optional.empty();
        }
        if (goal.isInGoal(ctx.playerFeet())) {
            resetEstimatedTicksToGoal();
            return Optional.of(0.0);
        }
        if (ticksElapsedSoFar == 0) {
            return Optional.empty();
        }
        double current = goal.heuristic(currentPos.x(), currentPos.y(), currentPos.z());
        double start = goal.heuristic(startPosition.x(), startPosition.y(), startPosition.z());
        if (current == start) {// can't check above because current and start can be equal even if currentPos and startPosition are not
            return Optional.empty();
        }
        double eta = Math.abs(current - goal.heuristic()) * ticksElapsedSoFar / Math.abs(start - current);
        return Optional.of(eta);
    }

    private void resetEstimatedTicksToGoal() {
        resetEstimatedTicksToGoal(expectedSegmentStart);
    }

    private void resetEstimatedTicksToGoal(BlockPos start) {
        ticksElapsedSoFar = 0;
        startPosition = start;
    }

    /**
     * See issue #209
     *
     * @return The starting {@link BlockPos} for a new path
     */
    public BlockPos pathStart() { // TODO move to a helper or util class
        BlockPos feet = ctx.playerFeet();
        if (!MovementHelper.canWalkOn(feet.below())) {
            if (ctx.player().isOnGround()) {
                double playerX = ctx.player().getX();
                double playerZ = ctx.player().getZ();
                ArrayList<BlockPos> closest = new ArrayList<>();
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        closest.add(new BlockPos(feet.x() + dx, feet.y(), feet.z() + dz));
                    }
                }
                closest.sort(Comparator.comparingDouble(pos -> ((pos.x() + 0.5D) - playerX) * ((pos.x() + 0.5D) - playerX) + ((pos.z() + 0.5D) - playerZ) * ((pos.z() + 0.5D) - playerZ)));
                for (int i = 0; i < 4; i++) {
                    BlockPos possibleSupport = closest.get(i);
                    double xDist = Math.abs((possibleSupport.x() + 0.5D) - playerX);
                    double zDist = Math.abs((possibleSupport.z() + 0.5D) - playerZ);
                    if (xDist > 0.8 && zDist > 0.8) {
                        // can't possibly be sneaking off of this one, we're too far away
                        continue;
                    }
                    if (MovementHelper.canWalkOn(possibleSupport.below()) && MovementHelper.canWalkThrough(possibleSupport) && MovementHelper.canWalkThrough(possibleSupport.above())) {
                        // this is plausible
                        //PATH_LOG.info("Faking path start assuming player is standing off the edge of a block");
                        return possibleSupport;
                    }
                }

            } else {
                // !onGround
                // we're in the middle of a jump
                if (MovementHelper.canWalkOn(feet.below().below())) {
                    //PATH_LOG.info("Faking path start assuming player is midair and falling");
                    return feet.below();
                }
                if (MovementHelper.isLiquid(feet.below())) {
                    return feet.below();
                }
            }
        }
        return feet;
    }

    private volatile long lastFailedPathTime = 0;
    private final AtomicInteger failedPathSearches = new AtomicInteger(0);

    /**
     * In a new thread, pathfind to target blockpos
     */
    private void findPathInNewThread(final BlockPos start, CalculationContext context) {
        // this must be called with synchronization on pathCalcLock!
        // actually, we can check this, muahaha
        if (!Thread.holdsLock(pathCalcLock)) {
            throw new IllegalStateException("Must be called with synchronization on pathCalcLock");
            // why do it this way? it's already indented so much that putting the whole thing in a synchronized(pathCalcLock) was just too much lol
        }
        if (inProgress != null) {
            throw new IllegalStateException("Already doing it"); // should have been checked by caller
        }
        Goal goal = this.goal;
        if (goal == null) {
            PATH_LOG.debug("no goal"); // TODO should this be an exception too? definitely should be checked by caller
            return;
        }
        long primaryTimeout;
        long failureTimeout;
        if (current == null) {
            primaryTimeout = CONFIG.client.extra.pathfinder.primaryTimeoutMs;
            failureTimeout = CONFIG.client.extra.pathfinder.failureTimeoutMs;
        } else {
            primaryTimeout = CONFIG.client.extra.pathfinder.planAheadPrimaryTimeoutMs;
            failureTimeout = CONFIG.client.extra.pathfinder.planAheadFailureTimeoutMs;
        }
        AbstractNodeCostSearch pathfinder = createPathfinder(start, goal, current == null ? null : current.getPath(), context);
        if (!Objects.equals(pathfinder.getGoal(), goal)) { // will return the exact same object if simplification didn't happen
            PATH_LOG.debug("Simplifying {} to GoalXZ due to distance", goal.getClass());
        }
        inProgress = pathfinder;
        pathSearchFuture = Baritone.getExecutor().submit(() -> {
            if (System.currentTimeMillis() - lastFailedPathTime < CONFIG.client.extra.pathfinder.failedPathSearchCooldownMs) {
                try {
                    Thread.sleep(CONFIG.client.extra.pathfinder.failedPathSearchCooldownMs - (System.currentTimeMillis() - lastFailedPathTime));
                } catch (InterruptedException ignored) {
                }
            }

            PATH_LOG.debug("Searching for path from {} to {}", start, goal);

            PathCalculationResult calcResult = pathfinder.calculate(primaryTimeout, failureTimeout);
            if (calcResult.getType() == PathCalculationResult.Type.FAILURE || calcResult.getType() == PathCalculationResult.Type.EXCEPTION) {
                lastFailedPathTime = System.currentTimeMillis();
                failedPathSearches.getAndIncrement();
            } else {
                failedPathSearches.set(0);
            }
            synchronized (pathPlanLock) {
                Optional<PathExecutor> executor = calcResult.getPath().map(p -> new PathExecutor(PathingBehavior.this, p));
                if (current == null) {
                    if (executor.isPresent()) {
                        if (executor.get().getPath().positions().contains(expectedSegmentStart)) {
                            queuePathEvent(PathEvent.CALC_FINISHED_NOW_EXECUTING);
                            current = executor.get();
                            resetEstimatedTicksToGoal(start);
                        } else {
                            PATH_LOG.debug("Warning: discarding orphan path segment with incorrect start");
                        }
                    } else {
                        if (calcResult.getType() != PathCalculationResult.Type.CANCELLATION && calcResult.getType() != PathCalculationResult.Type.EXCEPTION) {
                            // don't dispatch CALC_FAILED on cancellation
                            queuePathEvent(PathEvent.CALC_FAILED);
                        }
                    }
                } else {
                    if (next == null) {
                        if (executor.isPresent()) {
                            if (executor.get().getPath().getSrc().equals(current.getPath().getDest())) {
                                queuePathEvent(PathEvent.NEXT_SEGMENT_CALC_FINISHED);
                                next = executor.get();
                            } else {
                                PATH_LOG.debug("Warning: discarding orphan next segment with incorrect start");
                            }
                        } else {
                            queuePathEvent(PathEvent.NEXT_CALC_FAILED);
                        }
                    } else {
                        //throw new IllegalStateException("I have no idea what to do with this path");
                        // no point in throwing an exception here, and it gets it stuck with inProgress being not null
                        PATH_LOG.debug("Warning: PathingBehaivor illegal state! Discarding invalid path!");
                    }
                }
                if (current != null && current.getPath() != null) {
                    if (goal.isInGoal(current.getPath().getDest())) {
                        PATH_LOG.debug("Finished finding a path from {} to {}. {} nodes considered", start, goal, current.getPath().getNumNodesConsidered());
                    } else {
                        PATH_LOG.debug("Found path segment from {} towards {}. {} nodes considered", start, goal, current.getPath().getNumNodesConsidered());
                    }
                }
                synchronized (pathCalcLock) {
                    inProgress = null;
                }
            }
        });
    }

    private AbstractNodeCostSearch createPathfinder(BlockPos start, Goal goal, IPath previous, CalculationContext context) {
        Goal transformed = goal;
        if (CONFIG.client.extra.pathfinder.simplifyUnloadedYGoal && goal instanceof PosGoal posGoal) {
            BlockPos pos = posGoal.getGoalPos();
            if (!BlockStateInterface.worldContainsLoadedChunk(pos.x(), pos.z())) {
                transformed = new GoalXZ(pos.x(), pos.z());
            }
        }
        Favoring favoring = new Favoring(BARITONE.getPlayerContext(), previous, context);
        BlockPos feet = ctx.playerFeet();
        var realStart = new BlockPos(start);
        var sub = feet.subtract(realStart);
        if (feet.y() == realStart.y() && Math.abs(sub.x()) <= 1 && Math.abs(sub.z()) <= 1) {
            realStart = feet;
        }
        return new AStarPathFinder(realStart, start.x(), start.y(), start.z(), transformed, favoring, context);
    }

    /**
     * Returns the estimated remaining ticks in the current pathing
     * segment. Given that the return type is an optional, {@link Optional#empty()}
     * will be returned in the case that there is no current segment being pathed.
     *
     * @return The estimated remaining ticks in the current segment.
     */
    public Optional<Double> ticksRemainingInSegment() {
        return ticksRemainingInSegment(true);
    }

    /**
     * Returns the estimated remaining ticks in the current pathing
     * segment. Given that the return type is an optional, {@link Optional#empty()}
     * will be returned in the case that there is no current segment being pathed.
     *
     * @param includeCurrentMovement whether or not to include the entirety of the cost of the currently executing movement in the total
     * @return The estimated remaining ticks in the current segment.
     */
    public Optional<Double> ticksRemainingInSegment(boolean includeCurrentMovement) {
        PathExecutor current = getCurrent();
        if (current == null) {
            return Optional.empty();
        }
        int start = includeCurrentMovement ? current.getPosition() : current.getPosition() + 1;
        return Optional.of(current.getPath().ticksRemainingFrom(start));
    }

    public Optional<IPath> getPath() {
        return Optional.ofNullable(getCurrent()).map(PathExecutor::getPath);
    }

    /**
     * @return If there is a current path. Note that the path is not necessarily being executed, for example when there
     * is a pause in effect.
     * @see #isPathing()
     */
    public boolean hasPath() {
        return getCurrent() != null;
    }
}
