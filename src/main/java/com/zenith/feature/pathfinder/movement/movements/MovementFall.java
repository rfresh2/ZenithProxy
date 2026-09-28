package com.zenith.feature.pathfinder.movement.movements;

import com.zenith.feature.pathfinder.BlockStateInterface;
import com.zenith.feature.pathfinder.MutableMoveResult;
import com.zenith.feature.pathfinder.PathInput;
import com.zenith.feature.pathfinder.movement.*;
import com.zenith.feature.pathfinder.movement.MovementState.MovementTarget;
import com.zenith.feature.pathfinder.util.VecUtils;
import com.zenith.feature.player.Rotation;
import com.zenith.feature.player.RotationHelper;
import com.zenith.feature.player.World;
import com.zenith.mc.block.BlockPos;
import com.zenith.mc.block.BlockRegistry;
import com.zenith.mc.block.Direction;
import com.zenith.mc.block.properties.api.BlockStateProperties;
import lombok.ToString;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static com.zenith.feature.pathfinder.movement.ActionCosts.COST_INF;

@ToString(callSuper = true)
public class MovementFall extends Movement {

    public MovementFall(BlockPos src, BlockPos dest) {
        super(src, dest, MovementFall.buildPositionsToBreak(src, dest));
    }

    @Override
    public double calculateCost(CalculationContext context) {
        MutableMoveResult result = new MutableMoveResult();
        MovementDescend.cost(context, src.x(), src.y(), src.z(), dest.x(), dest.z(), result);
        if (result.y != dest.y()) {
            return COST_INF; // doesn't apply to us, this position is a descend not a fall
        }
        return result.cost;
    }

    @Override
    protected Set<BlockPos> calculateValidPositions() {
        Set<BlockPos> set = new HashSet<>();
        set.add(src);
        for (int y = src.y() - dest.y(); y >= 0; y--) {
            set.add(dest.above(y));
        }
        return set;
    }

    private boolean willPlaceBucket() {
        CalculationContext context = new CalculationContext();
        MutableMoveResult result = new MutableMoveResult();
        return MovementDescend.dynamicFallCost(context, src.x(), src.y(), src.z(), dest.x(), dest.z(), 0, context.getId(dest.x(), src.y() - 2, dest.z()), result);
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        BlockPos playerFeet = ctx.playerFeet();
        Vector3d blockPosCenter = VecUtils.getBlockPosCenter(dest);
        Vector2f rotVec = RotationHelper.rotationTo(blockPosCenter.getX(), blockPosCenter.getY(), blockPosCenter.getZ());
        Rotation toDest = new Rotation(rotVec.getX(), rotVec.getY());
        Rotation targetRotation = null;
        int destState = BlockStateInterface.getId(dest);
        boolean isWater = World.isWater(destState);
        if (!isWater && willPlaceBucket() && !playerFeet.equals(dest)) {
            return state.setStatus(MovementStatus.UNREACHABLE);
//            if (!Inventory.isHotbarSlot(ctx.player().getInventory().findSlotMatchingItem(STACK_BUCKET_WATER)) || ctx.world().dimension() == Level.NETHER) {
//                return state.setStatus(MovementStatus.UNREACHABLE);
//            }
//
//            if (ctx.player().position().y - dest.getY() < ctx.playerController().getBlockReachDistance() && !ctx.player().onGround()) {
//                ctx.player().getInventory().selected = ctx.player().getInventory().findSlotMatchingItem(STACK_BUCKET_WATER);
//
//                targetRotation = new Rotation(toDest.getYaw(), 90.0F);
//
//                if (ctx.isLookingAt(dest) || ctx.isLookingAt(dest.below())) {
//                    state.setInput(Input.CLICK_RIGHT, true);
//                }
//            }
        }
        if (targetRotation != null) {
            state.setTarget(new MovementTarget(targetRotation, true));
        } else {
            state.setTarget(new MovementTarget(toDest, false));
        }
        if (playerFeet.equals(dest) && (ctx.player().getY() - playerFeet.y() < 0.094 || isWater)) { // 0.094 because lilypads
            if (isWater) { // only match water, not flowing water (which we cannot pick up with a bucket)
                if (false) {
//                if (Inventory.isHotbarSlot(ctx.player().getInventory().findSlotMatchingItem(STACK_BUCKET_EMPTY))) {
//                    ctx.player().getInventory().selected = ctx.player().getInventory().findSlotMatchingItem(STACK_BUCKET_EMPTY);
//                    if (ctx.player().getDeltaMovement().y >= 0) {
//                        return state.setInput(Input.CLICK_RIGHT, true);
//                    } else {
//                        return state;
//                    }
                } else {
                    if (ctx.player().getVelocity().getY() >= 0) {
                        return state.setStatus(MovementStatus.SUCCESS);
                    } // don't else return state; we need to stay centered because this water might be flowing under the surface
                }
            } else {
                return state.setStatus(MovementStatus.SUCCESS);
            }
        }
        Vector3d destCenter = VecUtils.getBlockPosCenter(dest); // we are moving to the 0.5 center not the edge (like if we were falling on a ladder)
        if (Math.abs(ctx.player().getX() + ctx.player().getVelocity().getX() - destCenter.getX()) > 0.1 || Math.abs(ctx.player().getZ() + ctx.player().getVelocity().getZ() - destCenter.getZ()) > 0.1) {
            if (!ctx.player().isOnGround() && Math.abs(ctx.player().getVelocity().getY()) > 0.4) {
                state.setInput(PathInput.SNEAK, true);
            }
            state.setInput(PathInput.MOVE_FORWARD, true);
        }
        Vector3i avoid = Optional.ofNullable(avoid()).map(Direction::getNormal).orElse(null);
        if (avoid == null) {
            BlockPos pos = src.subtract(dest);
            avoid = Vector3i.from(pos.x(), pos.y(), pos.z());
        } else {
            double dist = Math.abs(avoid.getX() * (destCenter.getX() - avoid.getX() / 2.0 - ctx.player().getX())) + Math.abs(avoid.getZ() * (destCenter.getZ() - avoid.getZ() / 2.0 - ctx.player().getZ()));
            if (dist < 0.6) {
                state.setInput(PathInput.MOVE_FORWARD, true);
            } else if (!ctx.player().isOnGround()) {
                state.setInput(PathInput.SNEAK, false);
            }
        }
        if (targetRotation == null) {
            Vector3d destCenterOffset = Vector3d.from(destCenter.getX() + 0.125 * avoid.getX(), destCenter.getY(), destCenter.getZ() + 0.125 * avoid.getZ());
            var rotVec2 = RotationHelper.rotationTo(destCenterOffset.getX(), destCenterOffset.getY(), destCenterOffset.getZ());
            state.setTarget(new MovementTarget(new Rotation(rotVec2.getX(), rotVec2.getY()), false));
        }
        return state;
    }

    private Direction avoid() {
        BlockPos pos = ctx.playerFeet();
        for (int i = 0; i < 15; i++) {
            var block = World.getBlock(pos.x(), pos.y() - i, pos.z());
            if (block == BlockRegistry.LADDER) {
                return World.getBlockStateProperty(block, World.getBlockStateId(pos.x(), pos.y() - i, pos.z()), BlockStateProperties.HORIZONTAL_FACING);
            }
        }
        return null;
    }

    @Override
    public boolean safeToCancel(MovementState state) {
        // if we haven't started walking off the edge yet, or if we're in the process of breaking blocks before doing the fall
        // then it's safe to cancel this
        return ctx.playerFeet().equals(src) || state.getStatus() != MovementStatus.RUNNING;
    }

    private static BlockPos[] buildPositionsToBreak(BlockPos src, BlockPos dest) {
        BlockPos[] toBreak;
        int diffX = src.x() - dest.x();
        int diffZ = src.z() - dest.z();
        int diffY = Math.abs(src.y() - dest.y());
        toBreak = new BlockPos[diffY + 2];
        for (int i = 0; i < toBreak.length; i++) {
            toBreak[i] = new BlockPos(src.x() - diffX, src.y() + 1 - i, src.z() - diffZ);
        }
        return toBreak;
    }

    @Override
    protected boolean prepared(MovementState state) {
        if (state.getStatus() == MovementStatus.WAITING) {
            return true;
        }
        // only break if one of the first three needs to be broken
        // specifically ignore the last one which might be water
        for (int i = 0; i < 4 && i < positionsToBreak.length; i++) {
            if (!MovementHelper.canWalkThrough(positionsToBreak[i])) {
                return super.prepared(state);
            }
        }
        return true;
    }
}
