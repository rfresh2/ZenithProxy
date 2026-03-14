package com.zenith.feature.pathfinder.movement.movements;

import com.google.common.collect.ImmutableSet;
import com.zenith.feature.pathfinder.BlockStateInterface;
import com.zenith.feature.pathfinder.PathInput;
import com.zenith.feature.pathfinder.movement.*;
import com.zenith.feature.pathfinder.movement.MovementHelper.PlaceResult;
import com.zenith.mc.block.Block;
import com.zenith.mc.block.BlockPos;
import com.zenith.mc.block.BlockRegistry;
import com.zenith.mc.block.Direction;
import lombok.ToString;

import java.util.Set;

import static com.zenith.feature.pathfinder.movement.ActionCosts.*;

@ToString(callSuper = true)
public class MovementAscend extends Movement {

    private int ticksWithoutPlacement = 0;

    public MovementAscend(BlockPos src, BlockPos dest) {
        super(src, dest, new BlockPos[]{dest, src.above(2), dest.above()}, dest.below());
    }

    @Override
    public void reset() {
        super.reset();
        ticksWithoutPlacement = 0;
    }

    @Override
    public double calculateCost(CalculationContext context) {
        return cost(context, src.x(), src.y(), src.z(), dest.x(), dest.z());
    }

    @Override
    protected Set<BlockPos> calculateValidPositions() {
        BlockPos prior = new BlockPos(src.subtract(getDirection()).above()); // sometimes we back up to place the block, also sprint ascends, also skip descend to straight ascend
        return ImmutableSet.of(src,
                               src.above(),
                               dest,
                               prior,
                               prior.above()
        );
    }

    public static double cost(CalculationContext context, int x, int y, int z, int destX, int destZ) {
        int toPlace = context.getId(destX, y, destZ);
        double additionalPlacementCost = 0;
        if (!MovementHelper.canWalkOn(context, destX, y, destZ, toPlace)) {
            additionalPlacementCost = context.costOfPlacingAt(context, destX, y, destZ, toPlace);
            if (additionalPlacementCost >= COST_INF) {
                return COST_INF;
            }
            if (!MovementHelper.isReplaceable(destX, destZ, toPlace)) {
                return COST_INF;
            }
            boolean foundPlaceOption = false;
            for (int i = 0; i < 5; i++) {
                int againstX = destX + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[i].x();
                int againstY = y + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[i].y();
                int againstZ = destZ + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[i].z();
                if (againstX == x && againstZ == z) { // we might be able to backplace now, but it doesn't matter because it will have been broken by the time we'd need to use it
                    continue;
                }
                if (MovementHelper.canPlaceAgainst(againstX, againstY, againstZ)) {
                    foundPlaceOption = true;
                    break;
                }
            }
            if (!foundPlaceOption) { // didn't find a valid place =(
                return COST_INF;
            }
        }
        int srcUp2 = context.getId(x, y + 2, z); // used lower down anyway
        Block srcUp2Block = BlockStateInterface.getBlock(srcUp2);
        if (context.getBlock(x, y + 3, z).fallingBlock() && (MovementHelper.canWalkThrough(context, x, y + 1, z) || !(srcUp2Block.fallingBlock()))) {//it would fall on us and possibly suffocate us
            // HOWEVER, we assume that we're standing in the start position
            // that means that src and src.up(1) are both air
            // maybe they aren't now, but they will be by the time this starts
            // if the lower one is can't walk through and the upper one is falling, that means that by standing on src
            // (the presupposition of this Movement)
            // we have necessarily already cleared the entire FallingBlock stack
            // on top of our head

            // as in, if we have a block, then two FallingBlocks on top of it
            // and that block is x, y+1, z, and we'd have to clear it to even start this movement
            // we don't need to worry about those FallingBlocks because we've already cleared them
            return COST_INF;
            // you may think we only need to check srcUp2, not srcUp
            // however, in the scenario where glitchy world gen where unsupported sand / gravel generates
            // it's possible srcUp is AIR from the start, and srcUp2 is falling
            // and in that scenario, when we arrive and break srcUp2, that lets srcUp3 fall on us and suffocate us
        }
        int srcDown = context.getId(x, y - 1, z);
        Block srcDownBlock = BlockStateInterface.getBlock(srcDown);
        if (srcDownBlock == BlockRegistry.LADDER || srcDownBlock == BlockRegistry.VINE) {
            return COST_INF;
        }
        // we can jump from soul sand, but not from a bottom slab
        boolean jumpingFromBottomSlab = BlockStateInterface.isBottomSlab(srcDown);
        boolean jumpingToBottomSlab = BlockStateInterface.isBottomSlab(toPlace);
        if (jumpingFromBottomSlab && !jumpingToBottomSlab) {
            return COST_INF;// the only thing we can ascend onto from a bottom slab is another bottom slab
        }
        double walk;
        if (jumpingToBottomSlab) {
            if (jumpingFromBottomSlab) {
                walk = Math.max(JUMP_ONE_BLOCK_COST, WALK_ONE_BLOCK_COST); // we hit space immediately on entering this action
                walk += context.jumpPenalty;
            } else {
                walk = WALK_ONE_BLOCK_COST; // we don't hit space we just walk into the slab
            }
        } else {
            // jumpingFromBottomSlab must be false
            if (BlockStateInterface.getBlock(toPlace) == BlockRegistry.SOUL_SAND) {
                walk = WALK_ONE_OVER_SOUL_SAND_COST;
            } else {
                walk = Math.max(JUMP_ONE_BLOCK_COST, WALK_ONE_BLOCK_COST);
            }
            walk += context.jumpPenalty;
        }

        double totalCost = walk + additionalPlacementCost;
        // start with srcUp2 since we already have its state
        // includeFalling isn't needed because of the falling check above -- if srcUp3 is falling we will have already exited with COST_INF if we'd actually have to break it
        totalCost += MovementHelper.getMiningDurationTicks(context, x, y + 2, z, srcUp2, false);
        if (totalCost >= COST_INF) {
            return COST_INF;
        }
        totalCost += MovementHelper.getMiningDurationTicks(context, destX, y + 1, destZ, false);
        if (totalCost >= COST_INF) {
            return COST_INF;
        }
        totalCost += MovementHelper.getMiningDurationTicks(context, destX, y + 2, destZ, true);
        return totalCost;
    }

    @Override
    public MovementState updateState(MovementState state) {
        var isTouchingLiquid = MovementHelper.isPlayerTouchingLiquid();
        if (ctx.playerFeet().y() < src.y() && !isTouchingLiquid) {
            // this check should run even when in preparing state (breaking blocks)
            return state.setStatus(MovementStatus.UNREACHABLE);
        }
        super.updateState(state);
        // TODO incorporate some behavior from ActionClimb (specifically how it waited until it was at most 1.2 blocks away before starting to jump
        // for efficiency in ascending minimal height staircases, which is just repeated MovementAscend, so that it doesn't bonk its head on the ceiling repeatedly)
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        if (ctx.playerFeet().equals(dest) || ctx.playerFeet().equals(dest.offset(getDirection().below()))) {
            return state.setStatus(MovementStatus.SUCCESS);
        }

        int jumpingOnto = BlockStateInterface.getId(positionToPlace);
        if (!MovementHelper.canWalkOn(positionToPlace, jumpingOnto)) {
            ticksWithoutPlacement++;
            if (MovementHelper.attemptToPlaceABlock(state, dest.below(), false, true) == PlaceResult.READY_TO_PLACE) {
                state.setInput(PathInput.SNEAK, true);
                if (ctx.player().isSneaking()) {
                    state.setClickTarget(ctx.getSelectedBlock().orElse(null));
                    state.setInput(PathInput.RIGHT_CLICK_BLOCK, true);
                }
            }
            if (ticksWithoutPlacement > 10) {
                // After 10 ticks without placement, we might be standing in the way, move back
                state.setInput(PathInput.MOVE_BACK, true);
            }

            return state;
        }
        MovementHelper.moveTowards(state, dest);
        if (BlockStateInterface.isBottomSlab(jumpingOnto) && !BlockStateInterface.isBottomSlab(BlockStateInterface.getId(src.below()))) {
            return state; // don't jump while walking from a non double slab into a bottom slab
        }

        if (ctx.playerFeet().equals(src.above()) && !MovementHelper.isLiquid(ctx.playerFeet())) {
            // no need to hit space if we're already jumping
            return state;
        }

        int xAxis = Math.abs(src.x() - dest.x()); // either 0 or 1
        int zAxis = Math.abs(src.z() - dest.z()); // either 0 or 1
        double flatDistToNext = xAxis * Math.abs((dest.x() + 0.5D) - ctx.player().getX()) + zAxis * Math.abs((dest.z() + 0.5D) - ctx.player().getZ());
        double sideDist = zAxis * Math.abs((dest.x() + 0.5D) - ctx.player().getX()) + xAxis * Math.abs((dest.z() + 0.5D) - ctx.player().getZ());

        double lateralMotion = xAxis * ctx.player().getVelocity().getZ() + zAxis * ctx.player().getVelocity().getX();
        if (Math.abs(lateralMotion) > 0.1) {
            return state;
        }

        if (headBonkClear()) {
            MovementHelper.safeJump(state, ctx);
            return state;
        }

        if (flatDistToNext > 1.2 || sideDist > 0.2) {
            return state;
        }

        // Once we are pointing the right way and moving, start jumping
        // This is slightly more efficient because otherwise we might start jumping before moving, and fall down without moving onto the block we want to jump onto
        // Also wait until we are close enough, because we might jump and hit our head on an adjacent block
        MovementHelper.safeJump(state, ctx);
        return state;
    }

    public boolean headBonkClear() {
        BlockPos startUp = src.above(2);

        for (var dir : Direction.HORIZONTALS) {
            BlockPos check = startUp.relative(dir);
            if (!MovementHelper.canWalkThrough(check)) {
                // We might bonk our head
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean safeToCancel(MovementState state) {
        // if we had to place, don't allow pause
        return state.getStatus() != MovementStatus.RUNNING || ticksWithoutPlacement == 0;
    }
}
