package com.zenith.feature.pathfinder.movement;

import com.zenith.feature.pathfinder.BlockStateInterface;
import com.zenith.feature.pathfinder.PathInput;
import com.zenith.feature.pathfinder.PlayerContext;
import com.zenith.feature.pathfinder.util.RotationUtils;
import com.zenith.feature.pathfinder.util.VecUtils;
import com.zenith.feature.player.Rotation;
import com.zenith.feature.player.World;
import com.zenith.mc.block.BlockPos;
import com.zenith.mc.block.CollisionBox;
import com.zenith.mc.block.Direction;
import com.zenith.mc.block.LocalizedCollisionBox;
import lombok.ToString;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.jspecify.annotations.Nullable;

import java.util.*;

import static com.zenith.Globals.*;

@ToString
public abstract class Movement implements IMovement {
    public static final Direction[] HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN};

    private MovementState currentState = new MovementState().setStatus(MovementStatus.PREPPING);

    protected final BlockPos src;

    protected final BlockPos dest;

    private @Nullable Double cost;

    /**
     * The positions that need to be broken before this movement can ensue
     */
    protected final BlockPos[] positionsToBreak;

    /**
     * The position where we need to place a block before this movement can ensue
     */
    protected final BlockPos positionToPlace;
    private Boolean calculatedWhileLoaded;
    private Set<BlockPos> validPositionsCached = null;
    public List<BlockPos> toWalkIntoCached = null;
    public List<BlockPos> toBreakCached = null;
    public List<BlockPos> toPlaceCached = null;

    protected final PlayerContext ctx = PlayerContext.INSTANCE;

    protected Movement(BlockPos src, BlockPos dest, BlockPos[] toBreak, BlockPos toPlace) {
        this.src = src;
        this.dest = dest;
        this.positionsToBreak = toBreak;
        this.positionToPlace = toPlace;
    }

    protected Movement(BlockPos src, BlockPos dest, BlockPos[] toBreak) {
        this(src, dest, toBreak, null);
    }

    public abstract double calculateCost(CalculationContext context);

    protected abstract Set<BlockPos> calculateValidPositions();

    @Override
    public double getCost() throws NullPointerException {
        return cost;
    }

    public double getCost(CalculationContext context) {
        if (cost == null) {
            cost = calculateCost(context);
        }
        return cost;
    }

    public double recalculateCost(CalculationContext context) {
        cost = null;
        return getCost(context);
    }

    public void override(double cost) {
        this.cost = cost;
    }


    /**
     * Handles the execution of the latest Movement
     * State, and offers a Status to the calling class.
     *
     * @return Status
     */
    @Override
    public MovementStatus update() {
        currentState = updateState(currentState);

        if (MovementHelper.isLiquid(ctx.playerFeet()) && BOT.getY() < dest.y() + 0.6) {
            LocalizedCollisionBox predictedCb = BOT
                .getPlayerCollisionBox()
                .move(dest.x() - src.x(), dest.y() - src.y(), dest.z() - src.z());
            List<LocalizedCollisionBox> predicatedCollisions = new ArrayList<>(1);
            World.getSolidBlockCollisionBoxes(predictedCb, predicatedCollisions);
            boolean willCollide = false;
            for (LocalizedCollisionBox box : predicatedCollisions) {
                if (box.intersects(predictedCb)) {
                    willCollide = true;
                    break;
                }
            }
            if (!willCollide) {
                currentState.setInput(PathInput.JUMP, true);
            }
        }
//        if (ctx.player().isInWall()) {
//            ctx.getSelectedBlock().ifPresent(pos -> MovementHelper.switchToBestToolFor(ctx, BlockStateInterface.get(ctx, pos)));
//            currentState.setInput(Input.CLICK_LEFT, true);
//        }

        // todo: apply inputs
        // If the movement target has to force the new rotations, or we aren't using silent move, then force the rotations
        Rotation currentTargetRotation = currentState.getTarget().rotation();
        if (currentTargetRotation != null) {
            BARITONE.getLookBehavior().updateRotation(
                currentTargetRotation
            );
        }
        BARITONE.getInputOverrideHandler().clearAllKeys();
        currentState.getInputStates().forEach((input, forced) -> {
            BARITONE.getInputOverrideHandler().setInputForceState(input, forced);
        });
        BARITONE.getInputOverrideHandler().setClickTarget(currentState.getClickTarget());
        currentState.setClickTarget(null);
        currentState.getInputStates().clear();

        // If the current status indicates a completed movement
        if (currentState.getStatus().isComplete()) {
            BARITONE.getInputOverrideHandler().clearAllKeys();
        }

        return currentState.getStatus();
    }

    protected boolean prepared(MovementState state) {
        if (state.getStatus() == MovementStatus.WAITING) {
            return true;
        }
        boolean somethingInTheWay = false;
        for (BlockPos blockPos : positionsToBreak) {
            if (CONFIG.client.extra.pathfinder.pauseMiningForFallingBlocks) {
                var fallingEntityCb = new LocalizedCollisionBox(new CollisionBox(0, 1, 0, 1.1, 0, 1), blockPos.x(), blockPos.y(), blockPos.z());
                var cbResult = new ArrayList<LocalizedCollisionBox>();
                World.getEntityCollisionBoxes(fallingEntityCb, cbResult, entity -> entity.getEntityType() == EntityType.FALLING_BLOCK);
                if (!cbResult.isEmpty()) {
                    return false;
                }
            }
            if (!MovementHelper.canWalkThrough(blockPos)) { // can't break air, so don't try
                somethingInTheWay = true;
                MovementHelper.switchToBestToolFor(BlockStateInterface.getBlock(blockPos));
                Optional<Rotation> reachable = RotationUtils.reachable(ctx, blockPos, ctx.player().getBlockReachDistance(), false);
                if (reachable.isPresent()) {
                    Rotation rotTowardsBlock = reachable.get();
                    state.setTarget(new MovementState.MovementTarget(rotTowardsBlock, true));
                    if (ctx.isLookingAt(blockPos) || ctx.playerRotations().isReallyCloseTo(rotTowardsBlock)) {
                        state.setInput(PathInput.LEFT_CLICK_BLOCK, true);
                        state.setClickTarget(blockPos);
                    }
                    if (MovementHelper.isPlayerTouchingLiquid() && ctx.player().getY() < getDest().y()) {
                        state.setInput(PathInput.JUMP, true);
                    }
                    return false;
                }
                //get rekt minecraft
                //i'm doing it anyway
                //i dont care if theres snow in the way!!!!!!!
                //you dont own me!!!!
                state.setTarget(new MovementState.MovementTarget(RotationUtils.calcRotationFromVec3d(
                    ctx.playerHead(),
                    VecUtils.calculateBlockCenter(blockPos), ctx.playerRotations()), true)
                );
                // don't check selectedblock on this one, this is a fallback when we can't see any face directly, it's intended to be breaking the "incorrect" block
                state.setInput(PathInput.LEFT_CLICK_BLOCK, true);
                state.setClickTarget(blockPos);
                return false;
            }
        }
        if (somethingInTheWay) {
            // There's a block or blocks that we can't walk through, but we have no target rotation to reach any
            // So don't return true, actually set state to unreachable
            state.setStatus(MovementStatus.UNREACHABLE);
            return true;
        }
        return true;
    }

    @Override
    public BlockPos getSrc() {
        return src;
    }

    @Override
    public BlockPos getDest() {
        return dest;
    }

    @Override
    public void reset() {
        currentState = new MovementState().setStatus(MovementStatus.PREPPING);
    }

    /**
     * Calculate latest movement state. Gets called once a tick.
     *
     * @param state The current state
     * @return The new state
     */
    public MovementState updateState(MovementState state) {
        if (!prepared(state)) {
            return state.setStatus(MovementStatus.PREPPING);
        } else if (state.getStatus() == MovementStatus.PREPPING) {
            state.setStatus(MovementStatus.WAITING);
        }

        if (state.getStatus() == MovementStatus.WAITING) {
            state.setStatus(MovementStatus.RUNNING);
        }

        return state;
    }

    public boolean safeToCancel() {
        return safeToCancel(currentState);
    }

    protected boolean safeToCancel(MovementState currentState) {
        return true;
    }

    public void checkLoadedChunk() {
        calculatedWhileLoaded = BlockStateInterface.worldContainsLoadedChunk(dest.x(), dest.z());
    }

    @Override
    public boolean calculatedWhileLoaded() {
        return calculatedWhileLoaded;
    }

    @Override
    public void resetBlockCache() {
        toBreakCached = null;
        toPlaceCached = null;
        toWalkIntoCached = null;
    }

    @Override
    public BlockPos getDirection() {
        return getDest().subtract(getSrc());
    }

    public Set<BlockPos> getValidPositions() {
        if (validPositionsCached == null) {
            validPositionsCached = calculateValidPositions();
            Objects.requireNonNull(validPositionsCached);
        }
        return validPositionsCached;
    }

    protected boolean playerInValidPosition() {
        return getValidPositions().contains(ctx.playerFeet()) || getValidPositions().contains(BARITONE.getPathingBehavior().pathStart());
    }

    public List<BlockPos> toWalkInto() { // overridden by movementdiagonal
        if (toWalkIntoCached == null) {
            toWalkIntoCached = new ArrayList<>();
        }
        return toWalkIntoCached;
    }

    public List<BlockPos> toBreak() {
        if (toBreakCached != null) {
            return toBreakCached;
        }
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos positionToBreak : positionsToBreak) {
            if (!MovementHelper.canWalkThrough(positionToBreak.x(), positionToBreak.y(), positionToBreak.z())) {
                result.add(positionToBreak);
            }
        }
        toBreakCached = result;
        return result;
    }

    public List<BlockPos> toPlace() {
        if (toPlaceCached != null) {
            return toPlaceCached;
        }
        List<BlockPos> result = new ArrayList<>();
        if (positionToPlace != null && !MovementHelper.canWalkOn(positionToPlace.x(), positionToPlace.y(), positionToPlace.z())) {
            result.add(positionToPlace);
        }
        toPlaceCached = result;
        return result;
    }
}
