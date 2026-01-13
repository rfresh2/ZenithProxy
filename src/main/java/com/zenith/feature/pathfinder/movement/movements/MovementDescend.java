package com.zenith.feature.pathfinder.movement.movements;

import com.google.common.collect.ImmutableSet;
import com.zenith.feature.pathfinder.BlockStateInterface;
import com.zenith.feature.pathfinder.MutableMoveResult;
import com.zenith.feature.pathfinder.PathInput;
import com.zenith.feature.pathfinder.movement.*;
import com.zenith.feature.player.Rotation;
import com.zenith.feature.player.RotationHelper;
import com.zenith.feature.player.World;
import com.zenith.mc.block.Block;
import com.zenith.mc.block.BlockPos;
import com.zenith.mc.block.BlockRegistry;
import com.zenith.mc.block.BlockTags;
import lombok.ToString;
import org.cloudburstmc.math.vector.Vector2f;

import java.util.Set;

import static com.zenith.feature.pathfinder.movement.ActionCosts.*;

@ToString(callSuper = true)
public class MovementDescend extends Movement {

    private int numTicks = 0;
    public boolean forceSafeMode = false;

    public MovementDescend(BlockPos start, BlockPos end) {
        super(start, end, new BlockPos[]{end.above(2), end.above(), end}, end.below());
    }

    @Override
    public void reset() {
        super.reset();
        numTicks = 0;
        forceSafeMode = false;
    }

    /**
     * Called by PathExecutor if needing safeMode can only be detected with knowledge about the next movement
     */
    public void forceSafeMode() {
        forceSafeMode = true;
    }

    @Override
    public double calculateCost(CalculationContext context) {
        MutableMoveResult result = new MutableMoveResult();
        cost(context, src.x(), src.y(), src.z(), dest.x(), dest.z(), result);
        if (result.y != dest.y()) {
            return COST_INF; // doesn't apply to us, this position is a fall not a descend
        }
        return result.cost;
    }

    @Override
    protected Set<BlockPos> calculateValidPositions() {
        return ImmutableSet.of(src, dest.above(), dest);
    }

    public static void cost(CalculationContext context, int x, int y, int z, int destX, int destZ, MutableMoveResult res) {
        double totalCost = 0;
        int destDown = context.getId(destX, y - 1, destZ);
        totalCost += MovementHelper.getMiningDurationTicks(context, destX, y - 1, destZ, destDown, false);
        if (totalCost >= COST_INF) {
            return;
        }
        totalCost += MovementHelper.getMiningDurationTicks(context, destX, y, destZ, false);
        if (totalCost >= COST_INF) {
            return;
        }
        totalCost += MovementHelper.getMiningDurationTicks(context, destX, y + 1, destZ, true); // only the top block in the 3 we need to mine needs to consider the falling blocks above
        if (totalCost >= COST_INF) {
            return;
        }

        Block fromDown = context.getBlock(x, y - 1, z);
        if (fromDown.blockTags().contains(BlockTags.CLIMBABLE)) {
            return;
        }

        // A
        //SA
        // A
        // B
        // C
        // D
        //if S is where you start, B needs to be air for a movementfall
        //A is plausibly breakable by either descend or fall
        //C, D, etc determine the length of the fall

        int below = context.getId(destX, y - 2, destZ);
        if (!MovementHelper.canWalkOn(context, destX, y - 2, destZ, below)) {
            dynamicFallCost(context, x, y, z, destX, destZ, totalCost, below, res);
            return;
        }
        Block destDownBlock = BlockStateInterface.getBlock(destDown);
        if (destDownBlock.blockTags().contains(BlockTags.CLIMBABLE)) {
            return;
        }
        if (MovementHelper.canUseFrostWalker(context, destDown)) { // no need to check assumeWalkOnWater
            return; // the water will freeze when we try to walk into it
        }

        // we walk half the block plus 0.3 to get to the edge, then we walk the other 0.2 while simultaneously falling (math.max because of how it's in parallel)
        double walk = WALK_OFF_BLOCK_COST;
        if (fromDown == BlockRegistry.SOUL_SAND) {
            // use this ratio to apply the soul sand speed penalty to our 0.8 block distance
            walk *= WALK_ONE_OVER_SOUL_SAND_COST / WALK_ONE_BLOCK_COST;
        }
        totalCost += walk + Math.max(FALL_N_BLOCKS_COST[1], CENTER_AFTER_FALL_COST);
        res.x = destX;
        res.y = y - 1;
        res.z = destZ;
        res.cost = totalCost;
    }

    public static boolean dynamicFallCost(CalculationContext context, int x, int y, int z, int destX, int destZ, double frontBreak, int below, MutableMoveResult res) {
        if (frontBreak != 0 && context.getBlock(destX, y + 2, destZ).fallingBlock()) {
            // if frontBreak is 0 we can actually get through this without updating the falling block and making it actually fall
            // but if frontBreak is nonzero, we're breaking blocks in front, so don't let anything fall through this column,
            // and potentially replace the water we're going to fall into
            return false;
        }
        if (!MovementHelper.canWalkThrough(context, destX, y - 2, destZ, below)) {
            return false;
        }
        double costSoFar = 0;
        int effectiveStartHeight = y;
        for (int fallHeight = 3; true; fallHeight++) {
            int newY = y - fallHeight;
            if (newY < World.getCurrentDimension().minY()) {
                // when pathing in the end, where you could plausibly fall into the void
                // this check prevents it from getting the block at y=(min height - 1) and crashing
                return false;
            }
            boolean reachedMinimum = fallHeight >= context.minFallHeight;
            int ontoBlock = context.getId(destX, newY, destZ);
            int unprotectedFallHeight = fallHeight - (y - effectiveStartHeight); // equal to fallHeight - y + effectiveFallHeight, which is equal to -newY + effectiveFallHeight, which is equal to effectiveFallHeight - newY
            double tentativeCost = WALK_OFF_BLOCK_COST + FALL_N_BLOCKS_COST[unprotectedFallHeight] + frontBreak + costSoFar;
            if (reachedMinimum && MovementHelper.isWater(BlockStateInterface.getBlock(ontoBlock))) {
                if (!MovementHelper.canWalkThrough(context, destX, newY, destZ, ontoBlock)) {
                    return false;
                }
                if (context.assumeWalkOnWater) {
                    return false; // TODO fix
                }
                if (MovementHelper.isFlowing(destX, newY, destZ)) {
                    return false; // TODO flowing check required here?
                }
                if (!MovementHelper.canWalkOn(context, destX, newY - 1, destZ)) {
                    // we could punch right through the water into something else
                    return false;
                }
                // found a fall into water
                res.x = destX;
                res.y = newY;
                res.z = destZ;
                res.cost = tentativeCost;// TODO incorporate water swim up cost?
                return false;
            }
//            if (reachedMinimum && context.allowFallIntoLava && MovementHelper.isLava(ontoBlock)) {
//                // found a fall into lava
//                res.x = destX;
//                res.y = newY;
//                res.z = destZ;
//                res.cost = tentativeCost;
//                return false;
//            }
            if (unprotectedFallHeight <= 11 && (BlockStateInterface.getBlock(ontoBlock).blockTags().contains(BlockTags.CLIMBABLE))) {
                // if fall height is greater than or equal to 11, we don't actually grab on to vines or ladders. the more you know
                // this effectively "resets" our falling speed
                costSoFar += FALL_N_BLOCKS_COST[unprotectedFallHeight - 1];// we fall until the top of this block (not including this block)
                costSoFar += LADDER_DOWN_ONE_COST;
                effectiveStartHeight = newY;
                continue;
            }
            if (MovementHelper.canWalkThrough(context, destX, newY, destZ, ontoBlock)) {
                continue;
            }
            if (!MovementHelper.canWalkOn(context, destX, newY, destZ, ontoBlock)) {
                return false;
            }
            if (BlockStateInterface.isBottomSlab(ontoBlock)) {
                return false; // falling onto a half slab is really glitchy, and can cause more fall damage than we'd expect
            }
            if (reachedMinimum) {
                if (unprotectedFallHeight > context.maxFallHeightNoWater + 1) {
                    if (context.allowLongFall) {
                        res.x = destX;
                        res.y = newY + 1;
                        res.z = destZ;
                        int unsafeDistance = unprotectedFallHeight - (context.maxFallHeightNoWater + 1);
                        res.cost = tentativeCost + (context.longFallCostLogMultiplier * Math.log(unsafeDistance) + context.longFallCostAddCost);
                        return false;
                    } else {
                        return false;
                    }
                }
                res.x = destX;
                res.y = newY + 1;
                res.z = destZ;
                res.cost = tentativeCost;
                return false;
            }
            return false;
        }
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        BlockPos playerFeet = ctx.playerFeet();
        BlockPos fakeDest = new BlockPos(dest.x() * 2 - src.x(), dest.y(), dest.z() * 2 - src.z());
        if ((playerFeet.equals(dest) || playerFeet.equals(fakeDest)) && (MovementHelper.isLiquid(dest) || ctx.player().getY() - dest.y() < 0.5)) { // lilypads
            // Wait until we're actually on the ground before saying we're done because sometimes we continue to fall if the next action starts immediately
            return state.setStatus(MovementStatus.SUCCESS);
            /* else {
                // PATH_LOG.info(player().position().y + " " + playerFeet.getY() + " " + (player().position().y - playerFeet.getY()));
            }*/
        }
        if (safeMode()) {
            double destX = (src.x() + 0.5) * 0.17 + (dest.x() + 0.5) * 0.83;
            double destZ = (src.z() + 0.5) * 0.17 + (dest.z() + 0.5) * 0.83;
            Vector2f rot = RotationHelper.rotationTo(destX, dest.y(), destZ);
            state.setTarget(new MovementState.MovementTarget(new Rotation(rot.getX(), 0), false))
                .setInput(PathInput.MOVE_FORWARD, true);
            return state;
        }
        double diffX = ctx.player().getX() - (dest.x() + 0.5);
        double diffZ = ctx.player().getZ() - (dest.z() + 0.5);
        double ab = Math.sqrt(diffX * diffX + diffZ * diffZ);
        double x = ctx.player().getX() - (src.x() + 0.5);
        double z = ctx.player().getZ() - (src.z() + 0.5);
        double fromStart = Math.sqrt(x * x + z * z);
        if (!playerFeet.equals(dest) || ab > 0.25) {
            if (numTicks++ < 20 && fromStart < 1.25) {
                MovementHelper.moveTowards(state, fakeDest);
            } else {
                MovementHelper.moveTowards(state, dest);
            }
        }
        return state;
    }

    public boolean safeMode() {
        if (forceSafeMode) {
            return true;
        }
        // (dest - src) + dest is offset 1 more in the same direction
        // so it's the block we'd need to worry about running into if we decide to sprint straight through this descend
        BlockPos into = dest.subtract(src.below()).offset(dest);
        if (skipToAscend()) {
            // if dest extends into can't walk through, but the two above are can walk through, then we can overshoot and glitch in that weird way
            return true;
        }
        for (int y = 0; y <= 2; y++) { // we could hit any of the three blocks
            if (MovementHelper.avoidWalkingInto(BlockStateInterface.getBlock(into.above(y)))) {
                return true;
            }
        }
        return false;
    }

    public boolean skipToAscend() {
        BlockPos into = dest.subtract(src.below()).offset(dest);
        return !MovementHelper.canWalkThrough(into) && MovementHelper.canWalkThrough(into.above()) && MovementHelper.canWalkThrough(
            into.above(2));
    }
}
