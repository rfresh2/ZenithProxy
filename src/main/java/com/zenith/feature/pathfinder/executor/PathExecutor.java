package com.zenith.feature.pathfinder.executor;

import com.zenith.feature.pathfinder.BlockStateInterface;
import com.zenith.feature.pathfinder.PathInput;
import com.zenith.feature.pathfinder.PlayerContext;
import com.zenith.feature.pathfinder.behavior.PathingBehavior;
import com.zenith.feature.pathfinder.calc.AbstractNodeCostSearch;
import com.zenith.feature.pathfinder.calc.CutoffPath;
import com.zenith.feature.pathfinder.calc.IPath;
import com.zenith.feature.pathfinder.calc.SplicedPath;
import com.zenith.feature.pathfinder.movement.*;
import com.zenith.feature.pathfinder.movement.movements.*;
import com.zenith.feature.pathfinder.util.VecUtils;
import com.zenith.feature.player.Rotation;
import com.zenith.feature.player.RotationHelper;
import com.zenith.mc.block.BlockPos;
import com.zenith.util.math.MutableVec3d;
import com.zenith.util.struct.Pair;
import lombok.Getter;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.*;

import static com.zenith.Globals.*;
import static com.zenith.feature.pathfinder.movement.MovementStatus.*;

public class PathExecutor {
    private static final double MAX_MAX_DIST_FROM_PATH = 3;
    private static final double MAX_DIST_FROM_PATH = 2;

    /**
     * Default value is equal to 10 seconds. It's find to decrease it, but it must be at least 5.5s (110 ticks).
     * For more information, see issue #102.
     *
     * @see <a href="https://github.com/cabaletta/baritone/issues/102">Issue #102</a>
     * @see <a href="https://i.imgur.com/5s5GLnI.png">Anime</a>
     */
    private static final double MAX_TICKS_AWAY = 200;

    @Getter
    private final IPath path;
    private int pathPosition;
    private int ticksAway;
    private int ticksOnCurrent;
    private Double currentMovementOriginalCostEstimate;
    private Integer costEstimateIndex;
    private boolean failed;
    private boolean recalcBP = true;
    private HashSet<BlockPos> toBreak = new HashSet<>();
    private HashSet<BlockPos> toPlace = new HashSet<>();
    private HashSet<BlockPos> toWalkInto = new HashSet<>();

    private final PathingBehavior behavior;
    private final PlayerContext ctx = PlayerContext.INSTANCE;

    private boolean sprintNextTick;

    public PathExecutor(PathingBehavior behavior, IPath path) {
        this.behavior = behavior;
        this.path = path;
        this.pathPosition = 0;
    }

    /**
     * Tick this executor
     *
     * @return True if a movement just finished (and the player is therefore in a "stable" state, like,
     * not sneaking out over lava), false otherwise
     */
    public boolean onTick() {
        if (pathPosition == path.length() - 1) {
            pathPosition++;
        }
        if (pathPosition >= path.length()) {
            return true; // stop bugging me, I'm done
        }
        Movement movement = (Movement) path.movements().get(pathPosition);
        BlockPos whereAmI = ctx.playerFeet();
        if (!movement.getValidPositions().contains(whereAmI)) {
            for (int i = 0; i < pathPosition && i < path.length(); i++) {//this happens for example when you lag out and get teleported back a couple blocks
                if (((Movement) path.movements().get(i)).getValidPositions().contains(whereAmI)) {
                    int previousPos = pathPosition;
                    pathPosition = i;
                    for (int j = pathPosition; j <= previousPos; j++) {
                        path.movements().get(j).reset();
                    }
                    onChangeInPathPosition();
                    onTick();
                    return false;
                }
            }
            for (int i = pathPosition + 3; i < path.length() - 1; i++) { //dont check pathPosition+1. the movement tells us when it's done (e.g. sneak placing)
                // also don't check pathPosition+2 because reasons
                if (((Movement) path.movements().get(i)).getValidPositions().contains(whereAmI)) {
                    if (i - pathPosition > 2) {
                        PATH_LOG.debug("Skipping forward {} steps, to {}", i - pathPosition, i);
                    }
                    //PATH_LOG.info("Double skip sundae");
                    pathPosition = i - 1;
                    onChangeInPathPosition();
                    onTick();
                    return false;
                }
            }
        }
        Pair<Double, BlockPos> status = closestPathPos(path);
        if (possiblyOffPath(status, MAX_DIST_FROM_PATH)) {
            ticksAway++;
            PATH_LOG.debug("FAR AWAY FROM PATH FOR {} TICKS. Current distance: {}. Threshold: " + MAX_DIST_FROM_PATH, ticksAway, status.left());
            if (ticksAway > MAX_TICKS_AWAY) {
                PATH_LOG.debug("Too far away from path for too long, cancelling path");
                cancel();
                return false;
            }
        } else {
            ticksAway = 0;
        }
        if (possiblyOffPath(status, MAX_MAX_DIST_FROM_PATH)) { // ok, stop right away, we're way too far.
            PATH_LOG.debug("too far from path");
            cancel();
            return false;
        }
        for (int i = pathPosition - 10; i < pathPosition + 10; i++) {
            if (i < 0 || i >= path.movements().size()) {
                continue;
            }
            Movement m = (Movement) path.movements().get(i);
            List<BlockPos> prevBreak = m.toBreak();
            List<BlockPos> prevPlace = m.toPlace();
            List<BlockPos> prevWalkInto = m.toWalkInto();
            m.resetBlockCache();
            if (!prevBreak.equals(m.toBreak())) {
                recalcBP = true;
            }
            if (!prevPlace.equals(m.toPlace())) {
                recalcBP = true;
            }
            if (!prevWalkInto.equals(m.toWalkInto())) {
                recalcBP = true;
            }
        }
        if (recalcBP) {
            HashSet<BlockPos> newBreak = new HashSet<>();
            HashSet<BlockPos> newPlace = new HashSet<>();
            HashSet<BlockPos> newWalkInto = new HashSet<>();
            for (int i = pathPosition; i < path.movements().size(); i++) {
                Movement m = (Movement) path.movements().get(i);
                newBreak.addAll(m.toBreak());
                newPlace.addAll(m.toPlace());
                newWalkInto.addAll(m.toWalkInto());
            }
            toBreak = newBreak;
            toPlace = newPlace;
            toWalkInto = newWalkInto;
            recalcBP = false;
        }
        if (pathPosition < path.movements().size() - 1) {
            IMovement next = path.movements().get(pathPosition + 1);
            if (!BlockStateInterface.worldContainsLoadedChunk(next.getDest().x(), next.getDest().z())) {
                PATH_LOG.debug("Pausing since destination is at edge of loaded chunks");
                clearKeys();
                return true;
            }
        }
        boolean canCancel = movement.safeToCancel();
        if (costEstimateIndex == null || costEstimateIndex != pathPosition) {
            costEstimateIndex = pathPosition;
            // do this only once, when the movement starts, and deliberately get the cost as cached when this path was calculated, not the cost as it is right now
            currentMovementOriginalCostEstimate = movement.getCost();
            for (int i = 1; i < 5 && pathPosition + i < path.length() - 1; i++) {
                if (((Movement) path.movements().get(pathPosition + i)).calculateCost(behavior.secretInternalGetCalculationContext()) >= ActionCosts.COST_INF && canCancel) {
                    PATH_LOG.debug("Something has changed in the world and a future movement has become impossible. Cancelling.");
                    cancel();
                    return true;
                }
            }
        }
        double currentCost = movement.recalculateCost(behavior.secretInternalGetCalculationContext());
        if (currentCost >= ActionCosts.COST_INF && canCancel) {
            PATH_LOG.debug("Something has changed in the world and this movement has become impossible. Cancelling.");
            cancel();
            return true;
        }
        if (!movement.calculatedWhileLoaded() && currentCost - currentMovementOriginalCostEstimate > 10 && canCancel) {
            // don't do this if the movement was calculated while loaded
            // that means that this isn't a cache error, it's just part of the path interfering with a later part
            PATH_LOG.debug("Original cost {} current cost {}. Cancelling.", currentMovementOriginalCostEstimate, currentCost);
            cancel();
            return true;
        }
        if (shouldPause()) {
            PATH_LOG.debug("Pausing since current best path is a backtrack");
            clearKeys();
            return true;
        }
        MovementStatus movementStatus = movement.update();
        if (movementStatus == UNREACHABLE || movementStatus == FAILED) {
            PATH_LOG.debug("Movement returns status {}", movementStatus);
            cancel();
            return true;
        }
        if (movementStatus == SUCCESS) {
            //PATH_LOG.info("Movement done, next path");
            pathPosition++;
            onChangeInPathPosition();
            onTick();
            return true;
        } else {
            sprintNextTick = shouldSprintNextTick();
            if (sprintNextTick) {
                behavior.baritone.getInputOverrideHandler().setInputForceState(PathInput.SPRINT, true);
            }
            ticksOnCurrent++;
            double predictedMovementFinishTicks = currentMovementOriginalCostEstimate + 100;
            if (ticksOnCurrent > predictedMovementFinishTicks) {
                if (isMovementToBreakGoal(movement)) {
                    if (ticksOnCurrent > predictedMovementFinishTicks + (20 * 300)) {
                        PATH_LOG.info("This movement: {} to break goal has taken too long ({} ticks). Cancelling.",
                            movement.getClass().getSimpleName(),
                            ticksOnCurrent);
                        PATH_LOG.debug("{}", movement);
                        cancel();
                        return true;
                    }
                } else {
                    // only cancel if the total time has exceeded the initial estimate
                    // as you break the blocks required, the remaining cost goes down, to the point where
                    // ticksOnCurrent is greater than recalculateCost + 100
                    // this is why we cache cost at the beginning, and don't recalculate for this comparison every tick
                    PATH_LOG.info("This movement: {} has taken too long ({} ticks, expected {}). Cancelling.",
                                  movement.getClass().getSimpleName(),
                                  ticksOnCurrent,
                                  currentMovementOriginalCostEstimate);
                    PATH_LOG.debug("{}", movement);
                    cancel();
                    return true;
                }
            }
        }
        return canCancel; // movement is in progress, but if it reports cancellable, PathingBehavior is good to cut onto the next path
    }

    private boolean isMovementToBreakGoal(Movement movement) {
        if (!path.getGoal().isInGoal(movement.getDest())) return false;
        for (var breakPos : movement.toBreak()) {
            if (ctx.player().getInteractions().isDestroying(breakPos.x(), breakPos.y(), breakPos.z())) {
                return true;
            }
        }
        return false;
    }

    private Pair<Double, BlockPos> closestPathPos(IPath path) {
        double best = -1;
        BlockPos bestPos = null;
        for (IMovement movement : path.movements()) {
            for (BlockPos pos : ((Movement) movement).getValidPositions()) {
                double dist = VecUtils.entityDistanceToCenter(ctx.player(), pos);
                if (dist < best || best == -1) {
                    best = dist;
                    bestPos = pos;
                }
            }
        }
        return new Pair<>(best, bestPos);
    }

    private boolean shouldPause() {
        Optional<AbstractNodeCostSearch> current = behavior.getInProgress();
        if (current.isEmpty()) {
            return false;
        }
        if (!ctx.player().isOnGround()) {
            return false;
        }
        if (!MovementHelper.canWalkOn(ctx.playerFeet().below())) {
            // we're in some kind of sketchy situation, maybe parkouring
            return false;
        }
        if (!MovementHelper.canWalkThrough(ctx.playerFeet()) || !MovementHelper.canWalkThrough(ctx.playerFeet().above())) {
            // suffocating?
            return false;
        }
        if (!path.movements().get(pathPosition).safeToCancel()) {
            return false;
        }
        Optional<IPath> currentBest = current.get().bestPathSoFar();
        if (currentBest.isEmpty()) {
            return false;
        }
        List<BlockPos> positions = currentBest.get().positions();
        if (positions.size() < 3) {
            return false; // not long enough yet to justify pausing, its far from certain we'll actually take this route
        }
        // the first block of the next path will always overlap
        // no need to pause our very last movement when it would have otherwise cleanly exited with MovementStatus SUCCESS
        positions = positions.subList(1, positions.size());
        return positions.contains(ctx.playerFeet());
    }

    private boolean possiblyOffPath(Pair<Double, BlockPos> status, double leniency) {
        double distanceFromPath = status.left();
        if (distanceFromPath > leniency) {
            // when we're midair in the middle of a fall, we're very far from both the beginning and the end, but we aren't actually off path
            if (path.movements().get(pathPosition) instanceof MovementFall) {
                BlockPos fallDest = path.positions().get(pathPosition + 1); // .get(pathPosition) is the block we fell off of
                return VecUtils.entityFlatDistanceToCenter(ctx.player(), fallDest) >= leniency; // ignore Y by using flat distance
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * Regardless of current path position, snap to the current player feet if possible
     *
     * @return Whether or not it was possible to snap to the current player feet
     */
    public boolean snipsnapifpossible() {
        BlockPos playerPos = ctx.playerFeet();
        if (!ctx.player().isOnGround() && !MovementHelper.isLiquid(playerPos)) {
            // if we're falling in the air, and not in water, don't splice
            return false;
        } else {
            // we are either onGround or in liquid
            if (ctx.player().getVelocity().getY() < -0.1) {
                // if we are strictly moving downwards (not stationary)
                // we could be falling through water, which could be unsafe to splice
                return false; // so don't
            }
        }
        int index = path.positions().indexOf(playerPos);
        if (index == -1) {
            return false;
        }
        pathPosition = index; // jump directly to current position
        clearKeys();
        return true;
    }

    private boolean shouldSprintNextTick() {
        boolean requested = behavior.baritone.getInputOverrideHandler().isInputForcedDown(PathInput.SPRINT);

        behavior.baritone.getInputOverrideHandler().setInputForceState(PathInput.SPRINT, false);

        // first and foremost, if allowSprint is off, or if we don't have enough hunger, don't try and sprint
        if (!CONFIG.client.extra.pathfinder.allowSprint && CACHE.getPlayerCache().getThePlayer().getFood() > 6) {
            return false;
        }
        IMovement current = path.movements().get(pathPosition);

        // traverse requests sprinting, so we need to do this check first
        if (current instanceof MovementTraverse currentTraverse && pathPosition < path.length() - 3) {
            IMovement next = path.movements().get(pathPosition + 1);
            if (next instanceof MovementAscend nextAscend && canAscendSprint(ctx, currentTraverse, nextAscend, path.movements().get(pathPosition + 2))) {
                if (skipNow(ctx, currentTraverse)) {
                    pathPosition++;
                    onChangeInPathPosition();
                    onTick();
                    behavior.baritone.getInputOverrideHandler().setInputForceState(PathInput.JUMP, true);
                    return true;
                }
            }
        }

        // if the movement requested sprinting, then we're done
        if (requested) {
            return true;
        }

        // however, descend and ascend don't request sprinting, because they don't know the context of what movement comes after it
        if (current instanceof MovementDescend) {

            if (pathPosition < path.length() - 2) {
                // keep this out of onTick, even if that means a tick of delay before it has an effect
                IMovement next = path.movements().get(pathPosition + 1);
                if (MovementHelper.canUseFrostWalker(ctx, next.getDest().below())) {
                    // frostwalker only works if you cross the edge of the block on ground so in some cases we may not overshoot
                    // Since MovementDescend can't know the next movement we have to tell it
                    if (next instanceof MovementTraverse || next instanceof MovementParkour) {
                        boolean couldPlaceInstead = CONFIG.client.extra.pathfinder.allowPlace && behavior.baritone.getInventoryBehavior().hasGenericThrowaway() && next instanceof MovementParkour; // traverse doesn't react fast enough
                        // this is true if the next movement does not ascend or descends and goes into the same cardinal direction (N-NE-E-SE-S-SW-W-NW) as the descend
                        // in that case current.getDirection() is e.g. (0, -1, 1) and next.getDirection() is e.g. (0, 0, 3) so the cross product of (0, 0, 1) and (0, 0, 3) is taken, which is (0, 0, 0) because the vectors are colinear (don't form a plane)
                        // since movements in exactly the opposite direction (e.g. descend (0, -1, 1) and traverse (0, 0, -1)) would also pass this check we also have to rule out that case
                        // we can do that by adding the directions because traverse is always 1 long like descend and parkour can't jump through current.getSrc().down()
                        boolean sameFlatDirection = !current.getDirection().above().offset(next.getDirection()).equals(BlockPos.ZERO)
                            && current.getDirection().above().cross(next.getDirection()).equals(BlockPos.ZERO); // here's why you learn maths in school
                        if (sameFlatDirection && !couldPlaceInstead) {
                            ((MovementDescend) current).forceSafeMode();
                        }
                    }
                }
            }
            if (((MovementDescend) current).safeMode() && !((MovementDescend) current).skipToAscend()) {
                return false;
            }

            if (pathPosition < path.length() - 2) {
                IMovement next = path.movements().get(pathPosition + 1);
                if (next instanceof MovementAscend && current.getDirection().above().equals(next.getDirection().below())) {
                    // a descend then an ascend in the same direction
                    pathPosition++;
                    onChangeInPathPosition();
                    onTick();
                    // okay to skip clearKeys and / or onChangeInPathPosition here since this isn't possible to repeat, since it's asymmetric
                    return true;
                }
                if (canSprintFromDescendInto(ctx, current, next)) {

                    if (next instanceof MovementDescend && pathPosition < path.length() - 3) {
                        IMovement next_next = path.movements().get(pathPosition + 2);
                        if (next_next instanceof MovementDescend && !canSprintFromDescendInto(ctx, next, next_next)) {
                            return false;
                        }

                    }
                    if (ctx.playerFeet().equals(current.getDest())) {
                        pathPosition++;
                        onChangeInPathPosition();
                        onTick();
                    }

                    return true;
                }
                //PATH_LOG.info("Turning off sprinting " + movement + " " + next + " " + movement.getDirection() + " " + next.getDirection().down() + " " + next.getDirection().down().equals(movement.getDirection()));
            }
        }
        if (current instanceof MovementAscend && pathPosition != 0) {
            IMovement prev = path.movements().get(pathPosition - 1);
            if (prev instanceof MovementDescend && prev.getDirection().above().equals(current.getDirection().below())) {
                BlockPos center = current.getSrc().above();
                // playerFeet adds 0.1251 to account for soul sand
                // farmland is 0.9375
                // 0.07 is to account for farmland
                if (ctx.player().getY() >= center.y() - 0.07) {
                    behavior.baritone.getInputOverrideHandler().setInputForceState(PathInput.JUMP, false);
                    return true;
                }
            }
            if (pathPosition < path.length() - 2 && prev instanceof MovementTraverse && canAscendSprint(ctx, (MovementTraverse) prev, (MovementAscend) current, path.movements().get(pathPosition + 1))) {
                return true;
            }
        }
        if (current instanceof MovementFall) {
            Pair<Vector3d, BlockPos> data = overrideFall((MovementFall) current);
            if (data != null) {
                BlockPos fallDest = new BlockPos(data.right());
                if (!path.positions().contains(fallDest)) {
                    throw new IllegalStateException();
                }
                if (ctx.playerFeet().equals(fallDest)) {
                    pathPosition = path.positions().indexOf(fallDest);
                    onChangeInPathPosition();
                    onTick();
                    return true;
                }
                clearKeys();
                Vector3d a = data.left();
                var rotVec = RotationHelper.rotationTo(a.getX(), a.getY(), a.getZ());
                behavior.baritone.getLookBehavior().updateRotation(new Rotation(rotVec.getX(), rotVec.getY()));
                behavior.baritone.getInputOverrideHandler().setInputForceState(PathInput.MOVE_FORWARD, true);
                return true;
            }
        }
        return false;
    }

    private Pair<Vector3d, BlockPos> overrideFall(MovementFall movement) {
        BlockPos dir = movement.getDirection();
        if (dir.y() < -3) {
            return null;
        }
        if (!movement.toBreakCached.isEmpty()) {
            return null; // it's breaking
        }
        BlockPos flatDir = new BlockPos(dir.x(), 0, dir.z());
        int i;
        outer:
        for (i = pathPosition + 1; i < path.length() - 1 && i < pathPosition + 3; i++) {
            IMovement next = path.movements().get(i);
            if (!(next instanceof MovementTraverse)) {
                break;
            }
            if (!flatDir.equals(next.getDirection())) {
                break;
            }
            for (int y = next.getDest().y(); y <= movement.getSrc().y() + 1; y++) {
                BlockPos chk = new BlockPos(next.getDest().x(), y, next.getDest().z());
                if (!MovementHelper.fullyPassable(chk)) {
                    break outer;
                }
            }
            if (!MovementHelper.canWalkOn(next.getDest().below())) {
                break;
            }
        }
        i--;
        if (i == pathPosition) {
            return null; // no valid extension exists
        }
        double len = i - pathPosition - 0.4;
        return new Pair<>(
            Vector3d.from(flatDir.x() * len + movement.getDest().x() + 0.5, movement.getDest().y(), flatDir.z() * len + movement.getDest().z() + 0.5),
            movement.getDest().offset(flatDir.x() * (i - pathPosition), 0, flatDir.z() * (i - pathPosition)));
    }

    private static boolean skipNow(PlayerContext ctx, MovementTraverse current) {
        BlockPos moveDirection = current.getDirection();
        double offTarget = Math.abs(moveDirection.x() * (current.getSrc().z() + 0.5D - ctx.player().getZ())) + Math.abs(
            moveDirection.z() * (current.getSrc().x() + 0.5D - ctx.player().getX()));
        if (offTarget > 0.1) {
            return false;
        }
        // check that we are not moving with a significant sideways velocity
        MutableVec3d playerVelocity = ctx.player().getVelocity();
        Vector3i toDest = current.getSrc().directionTo(current.getDest());
        if (toDest.getX() != 0) {
            if (Math.abs(playerVelocity.getZ()) > 0.03) {
                return false;
            }
        } else if (toDest.getZ() != 0) {
            if (Math.abs(playerVelocity.getX()) > 0.03) {
                return false;
            }
        }

        // we are centered
        BlockPos headBonk = current.getSrc().subtract(moveDirection).above(2);
        if (MovementHelper.fullyPassable(headBonk)) {
            return true;
        }
        // wait 0.3
        double flatDist = Math.abs(moveDirection.x() * (headBonk.x() + 0.5D - ctx.player().getX())) + Math.abs(moveDirection.z() * (headBonk.z() + 0.5 - ctx.player().getZ()));
        return flatDist > 0.8;
    }

    private static boolean canAscendSprint(PlayerContext ctx, MovementTraverse current, MovementAscend next, IMovement nextnext) {
        if (!current.getDirection().equals(next.getDirection().below())) {
            return false;
        }
        if (nextnext.getDirection().x() != next.getDirection().x() || nextnext.getDirection().z() != next.getDirection().z()) {
            return false;
        }
        if (!MovementHelper.canWalkOn(current.getDest().below())) {
            return false;
        }
        if (!MovementHelper.canWalkOn(next.getDest().below())) {
            return false;
        }
        if (!next.toBreakCached.isEmpty()) {
            return false; // it's breaking
        }
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 3; y++) {
                BlockPos chk = current.getSrc().above(y);
                if (x == 1) {
                    chk = chk.offset(current.getDirection());
                }
                if (!MovementHelper.fullyPassable(chk)) {
                    return false;
                }
            }
        }
        if (MovementHelper.avoidWalkingInto(BlockStateInterface.getBlock(current.getSrc().above(3)))) {
            return false;
        }
        return !MovementHelper.avoidWalkingInto(BlockStateInterface.getBlock(next.getDest().above(2))); // codacy smh my head
    }

    private static boolean canSprintFromDescendInto(PlayerContext ctx, IMovement current, IMovement next) {
        if (next instanceof MovementDescend && next.getDirection().equals(current.getDirection())) {
            return true;
        }
        if (!MovementHelper.canWalkOn(current.getDest().offset(current.getDirection()))) {
            return false;
        }
        if (next instanceof MovementTraverse && next.getDirection().equals(current.getDirection())) {
            return true;
        }
        return next instanceof MovementDiagonal;
    }

    private void onChangeInPathPosition() {
        clearKeys();
        ticksOnCurrent = 0;
    }

    private void clearKeys() {
        // i'm just sick and tired of this snippet being everywhere lol
        behavior.baritone.getInputOverrideHandler().clearAllKeys();
    }

    private void cancel() {
        clearKeys();
        pathPosition = path.length() + 3;
        failed = true;
    }

    public int getPosition() {
        return pathPosition;
    }

    public PathExecutor trySplice(PathExecutor next) {
        if (next == null) {
            return cutIfTooLong();
        }
        return SplicedPath.trySplice(path, next.path, false).map(path -> {
            if (!path.getDest().equals(next.getPath().getDest())) {
                throw new IllegalStateException();
            }
            PathExecutor ret = new PathExecutor(behavior, path);
            ret.pathPosition = pathPosition;
            ret.currentMovementOriginalCostEstimate = currentMovementOriginalCostEstimate;
            ret.costEstimateIndex = costEstimateIndex;
            ret.ticksOnCurrent = ticksOnCurrent;
            return ret;
        }).orElseGet(this::cutIfTooLong); // dont actually call cutIfTooLong every tick if we won't actually use it, use a method reference
    }

    private PathExecutor cutIfTooLong() {
        if (pathPosition > 300) {
            int cutoffAmt = 50;
            CutoffPath newPath = new CutoffPath(path, cutoffAmt, path.length() - 1);
            if (!newPath.getDest().equals(path.getDest())) {
                throw new IllegalStateException();
            }
            PATH_LOG.debug("Discarding earliest segment movements, length cut from {} to {}", path.length(), newPath.length());
            PathExecutor ret = new PathExecutor(behavior, newPath);
            ret.pathPosition = pathPosition - cutoffAmt;
            ret.currentMovementOriginalCostEstimate = currentMovementOriginalCostEstimate;
            if (costEstimateIndex != null) {
                ret.costEstimateIndex = costEstimateIndex - cutoffAmt;
            }
            ret.ticksOnCurrent = ticksOnCurrent;
            return ret;
        }
        return this;
    }

    public boolean failed() {
        return failed;
    }

    public boolean finished() {
        return pathPosition >= path.length();
    }

    public Set<BlockPos> toBreak() {
        return Collections.unmodifiableSet(toBreak);
    }

    public Set<BlockPos> toPlace() {
        return Collections.unmodifiableSet(toPlace);
    }

    public Set<BlockPos> toWalkInto() {
        return Collections.unmodifiableSet(toWalkInto);
    }

    public boolean isSprinting() {
        return sprintNextTick;
    }
}
