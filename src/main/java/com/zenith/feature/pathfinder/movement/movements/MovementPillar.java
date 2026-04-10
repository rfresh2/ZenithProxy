package com.zenith.feature.pathfinder.movement.movements;

import com.google.common.collect.ImmutableSet;
import com.zenith.feature.pathfinder.BlockStateInterface;
import com.zenith.feature.pathfinder.PathInput;
import com.zenith.feature.pathfinder.movement.*;
import com.zenith.feature.pathfinder.util.RotationUtils;
import com.zenith.feature.pathfinder.util.VecUtils;
import com.zenith.feature.player.Rotation;
import com.zenith.feature.player.World;
import com.zenith.mc.block.Block;
import com.zenith.mc.block.BlockPos;
import com.zenith.mc.block.BlockRegistry;
import com.zenith.mc.block.BlockTags;
import lombok.ToString;
import org.cloudburstmc.math.vector.Vector3d;

import java.util.Set;

import static com.zenith.Globals.BARITONE;
import static com.zenith.feature.pathfinder.movement.ActionCosts.*;

@ToString(callSuper = true)
public class MovementPillar extends Movement {

    public MovementPillar(BlockPos start, BlockPos end) {
        super(start, end, new BlockPos[]{start.above(2)}, start);
    }

    @Override
    public double calculateCost(CalculationContext context) {
        return cost(context, src.x(), src.y(), src.z());
    }

    @Override
    protected Set<BlockPos> calculateValidPositions() {
        return ImmutableSet.of(src, dest);
    }

    public static double cost(CalculationContext context, int x, int y, int z) {
        int fromState = context.getId(x, y, z);
        Block fromBlock = BlockStateInterface.getBlock(fromState);
        boolean ladder = fromBlock == BlockRegistry.LADDER
            || fromBlock == BlockRegistry.VINE
            || fromBlock == BlockRegistry.TWISTING_VINES
            || fromBlock == BlockRegistry.TWISTING_VINES_PLANT
            || fromBlock == BlockRegistry.WEEPING_VINES
            || fromBlock == BlockRegistry.WEEPING_VINES_PLANT;
        int fromDown = context.getId(x, y - 1, z);
        Block fromDownBlock = BlockStateInterface.getBlock(fromDown);
        if (!ladder) {
            if (fromDownBlock.blockTags().contains(BlockTags.CLIMBABLE)) {
                return COST_INF; // can't pillar from a ladder or vine onto something that isn't also climbable
            }
            if (BlockStateInterface.isBottomSlab(fromDown)) {
                return COST_INF; // can't pillar up from a bottom slab onto a non ladder
            }
        }
//        if (from == BlockRegistry.VINE && !hasAgainst(context, x, y, z)) { // TODO this vine can't be climbed, but we could place a pillar still since vines are replacable, no? perhaps the pillar jump would be impossible because of the slowdown actually.
//            return COST_INF;
//        }
        int toBreak = context.getId(x, y + 2, z);
        Block toBreakBlock = BlockStateInterface.getBlock(toBreak);
        if (toBreakBlock.blockTags().contains(BlockTags.FENCE_GATES)) { // see issue #172
            return COST_INF;
        }
        Block srcUp = null;
        if (MovementHelper.isLiquid(toBreakBlock) && MovementHelper.isLiquid(fromBlock)) { // TODO should this also be allowed if toBreakBlock is air?
            srcUp = context.getBlock(x, y + 1, z);
            if (MovementHelper.isLiquid(srcUp)) {
                if (MovementHelper.isWater(srcUp)) {
                    return LADDER_UP_ONE_COST; // allow ascending pillars of water, but only if we're already in one
                } else {
                    // lava
                    return LADDER_UP_ONE_COST * 10;
                }
            }
        }
        double placeCost = 0;
        if (!ladder) {
            // we need to place a block where we started to jump on it
            placeCost = context.costOfPlacingAt(context, x, y, z, fromState);
            if (placeCost >= COST_INF) {
                return COST_INF;
            }
            if (fromDownBlock.isAir()) {
                placeCost += 0.1; // slightly (1/200th of a second) penalize pillaring on what's currently air
            }
        }
        if ((MovementHelper.isLiquid(fromBlock) && !MovementHelper.canPlaceAgainst(fromDown)) || (MovementHelper.isLiquid(fromDownBlock) && context.assumeWalkOnWater)) {
            // otherwise, if we're standing in water, we cannot pillar
            // if we're standing on water and assumeWalkOnWater is true, we cannot pillar
            // if we're standing on water and assumeWalkOnWater is false, we must have ascended to here, or sneak backplaced, so it is possible to pillar again
            return COST_INF;
        }
        if ((fromBlock == BlockRegistry.LILY_PAD || fromBlock.blockTags().contains(BlockTags.WOOL_CARPETS)) && World.isFluid(fromDownBlock)) {
            // to ascend here we'd have to break the block we are standing on
            return COST_INF;
        }
        double hardness = MovementHelper.getMiningDurationTicks(context, x, y + 2, z, toBreak, true);
        if (hardness >= COST_INF) {
            return COST_INF;
        }
        if (hardness != 0) {
            if (toBreakBlock == BlockRegistry.LADDER
                || toBreakBlock == BlockRegistry.VINE
                || toBreakBlock == BlockRegistry.TWISTING_VINES
                || toBreakBlock == BlockRegistry.TWISTING_VINES_PLANT
                || toBreakBlock == BlockRegistry.WEEPING_VINES
                || toBreakBlock == BlockRegistry.WEEPING_VINES_PLANT
            ) {
                hardness = 0; // we won't actually need to break the ladder / vine because we're going to use it
            } else {
                var check = context.getBlock(x, y + 3, z); // the block on top of the one we're going to break, could it fall on us?
                if (check.fallingBlock()) {
                    // see MovementAscend's identical check for breaking a falling block above our head
                    if (srcUp == null) {
                        srcUp = context.getBlock(x, y + 1, z);
                    }
                    if (!toBreakBlock.fallingBlock() || !srcUp.fallingBlock()) {
                        return COST_INF;
                    }
                }
                // this is commented because it may have had a purpose, but it's very unclear what it was. it's from the minebot era.
                //if (!MovementHelper.canWalkOn(context, chkPos, check) || MovementHelper.canWalkThrough(context, chkPos, check)) {//if the block above where we want to break is not a full block, don't do it
                // TODO why does canWalkThrough mean this action is COST_INF?
                // FallingBlock makes sense, and !canWalkOn deals with weird cases like if it were lava
                // but I don't understand why canWalkThrough makes it impossible
                //    return COST_INF;
                //}
            }
        }
        if (ladder) {
            return LADDER_UP_ONE_COST + hardness * 5;
        } else {
            return JUMP_ONE_BLOCK_COST + placeCost + context.jumpPenalty + hardness;
        }
    }

//    public static boolean hasAgainst(CalculationContext context, int x, int y, int z) {
//        return MovementHelper.isBlockNormalCube(context.get(x + 1, y, z)) ||
//                MovementHelper.isBlockNormalCube(context.get(x - 1, y, z)) ||
//                MovementHelper.isBlockNormalCube(context.get(x, y, z + 1)) ||
//                MovementHelper.isBlockNormalCube(context.get(x, y, z - 1));
//    }
//
//    public static BlockPos getAgainst(CalculationContext context, BlockPos vine) {
//        if (MovementHelper.isBlockNormalCube(context.getId(vine.north()))) {
//            return vine.north();
//        }
//        if (MovementHelper.isBlockNormalCube(context.getId(vine.south()))) {
//            return vine.south();
//        }
//        if (MovementHelper.isBlockNormalCube(context.getId(vine.east()))) {
//            return vine.east();
//        }
//        if (MovementHelper.isBlockNormalCube(context.getId(vine.west()))) {
//            return vine.west();
//        }
//        return null;
//    }

    public static BlockPos getAgainst(BlockPos vine) {
        if (MovementHelper.isBlockNormalCube(BlockStateInterface.getId(vine.north()))) {
            return vine.north();
        }
        if (MovementHelper.isBlockNormalCube(BlockStateInterface.getId(vine.south()))) {
            return vine.south();
        }
        if (MovementHelper.isBlockNormalCube(BlockStateInterface.getId(vine.east()))) {
            return vine.east();
        }
        if (MovementHelper.isBlockNormalCube(BlockStateInterface.getId(vine.west()))) {
            return vine.west();
        }
        return null;
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        if (ctx.playerFeet().y() < src.y()) {
            return state.setStatus(MovementStatus.UNREACHABLE);
        }

        int fromDown = BlockStateInterface.getId(src);
        Block fromDownBlock = BlockStateInterface.getBlock(fromDown);
        if (MovementHelper.isLiquid(fromDownBlock) && MovementHelper.isLiquid(dest)) {
            var headBonkPos = dest.above(1);
            var headBonkBlock = BlockStateInterface.getBlock(headBonkPos);
            var headBonkPos2 = dest.above(2);
            var headBonkBlock2 = BlockStateInterface.getBlock(headBonkPos2);
            BlockPos breakHeadBonk = null;
            if (!MovementHelper.isLiquid(headBonkBlock) && !MovementHelper.canWalkThrough(headBonkPos)) {
                breakHeadBonk = headBonkPos;
            } else if (!MovementHelper.isLiquid(headBonkBlock2) && !MovementHelper.canWalkThrough(headBonkPos2)) {
                breakHeadBonk = headBonkPos2;
            }
            if (breakHeadBonk != null) {
                state.setTarget(new MovementState.MovementTarget(RotationUtils.calcRotationFromVec3d(ctx.playerHead(), VecUtils.calculateBlockCenter(breakHeadBonk), ctx.playerRotations()), false));
                MovementHelper.switchToBestToolFor(World.getBlock(breakHeadBonk));
                state.setInput(PathInput.LEFT_CLICK_BLOCK, true);
                state.setClickTarget(breakHeadBonk);
                state.setInput(PathInput.JUMP, true);
                return state;
            }
            // stay centered while swimming up a water column
            state.setTarget(new MovementState.MovementTarget(RotationUtils.calcRotationFromVec3d(ctx.playerHead(), VecUtils.getBlockPosCenter(dest), ctx.playerRotations()), false));
            Vector3d destCenter = VecUtils.getBlockPosCenter(dest);
            if (Math.abs(ctx.player().getX() - destCenter.getX()) > 0.2 || Math.abs(ctx.player().getZ() - destCenter.getZ()) > 0.2) {
                state.setInput(PathInput.MOVE_FORWARD, true);
            }
            if (ctx.playerFeet().equals(dest)) {
                return state.setStatus(MovementStatus.SUCCESS);
            }
            return state;
        }
        boolean climbing = fromDownBlock.blockTags().contains(BlockTags.CLIMBABLE);
        Rotation rotation = RotationUtils.calcRotationFromVec3d(
            ctx.playerHead(),
            VecUtils.getBlockPosCenter(positionToPlace),
            ctx.playerRotations());
        if (!climbing) {
            state.setTarget(new MovementState.MovementTarget(ctx.playerRotations().withPitch(rotation.pitch()), true));
        }

        boolean blockIsThere = MovementHelper.canWalkOn(src) || climbing;
        if (climbing) {
            if (ctx.playerFeet().y() == dest.y()) {
                return state.setStatus(MovementStatus.SUCCESS);
            }
            state.setInput(PathInput.JUMP, true);
            MovementHelper.moveToBlockCenter(state, ctx, src);
            return state;
        } else {
            // Get ready to place a throwaway block
            if (!BARITONE.getInventoryBehavior().selectThrowawayForLocation(true, src.x(), src.y(), src.z())) {
                return state.setStatus(MovementStatus.UNREACHABLE);
            }


            state.setInput(PathInput.SNEAK, ctx.player().getY() > dest.y() || ctx.player().getY() < src.y() + 0.2D); // delay placement by 1 tick for ncp compatibility
            // since (lower down) we only right click once player.isSneaking, and that happens the tick after we request to sneak

            double diffX = ctx.player().getX() - (dest.x() + 0.5);
            double diffZ = ctx.player().getZ() - (dest.z() + 0.5);
            double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
            double flatMotion = Math.sqrt(ctx.player().getVelocity().getX() * ctx.player().getVelocity().getX() + ctx.player().getVelocity().getZ() * ctx.player().getVelocity().getZ());
            if (dist > 0.17) {//why 0.17? because it seemed like a good number, that's why
                //[explanation added after baritone port lol] also because it needs to be less than 0.2 because of the 0.3 sneak limit
                //and 0.17 is reasonably less than 0.2

                // If it's been more than forty ticks of trying to jump and we aren't done yet, go forward, maybe we are stuck
                state.setInput(PathInput.MOVE_FORWARD, true);

                // revise our target to both yaw and pitch if we're going to be moving forward
                state.setTarget(new MovementState.MovementTarget(rotation, true));
            } else if (flatMotion < 0.05) {
                // If our Y coordinate is above our goal, stop jumping
                state.setInput(PathInput.JUMP, ctx.player().getY() < dest.y());
            }


            if (!blockIsThere) {
                int frState = BlockStateInterface.getId(src);
                Block fr = BlockStateInterface.getBlock(frState);
                // TODO: Evaluate usage of getMaterial().isReplaceable()
                if (!(fr.isAir() || MovementHelper.isReplaceable(src.x(), src.y(), frState))) {
                    RotationUtils.reachable(ctx, src, ctx.player().getBlockReachDistance())
                            .map(rot -> new MovementState.MovementTarget(rot, true))
                            .ifPresent(state::setTarget);
                    state.setInput(PathInput.JUMP, false); // breaking is like 5x slower when you're jumping
                    state.setClickTarget(src);
                    state.setInput(PathInput.LEFT_CLICK_BLOCK, true);
                    blockIsThere = false;
                } else if (ctx.player().isSneaking() && (ctx.isLookingAt(src.below()) || ctx.isLookingAt(src)) && ctx.player().getY() > dest.y() + 0.1) {
                    state.setClickTarget(ctx.getSelectedBlock().orElse(null));
                    state.setInput(PathInput.RIGHT_CLICK_BLOCK, true);
                }
            }
        }

        // If we are at our goal and the block below us is placed
        if (ctx.playerFeet().equals(dest) && blockIsThere) {
            return state.setStatus(MovementStatus.SUCCESS);
        }

        return state;
    }

    @Override
    protected boolean prepared(MovementState state) {
//        if (ctx.playerFeet().equals(src) || ctx.playerFeet().equals(src.below())) {
//            Block block = BlockStateInterface.getBlock(src.below());
//            if (block.blockTags().contains(BlockTags.CLIMBABLE)) {
//                state.setInput(PathInput.SNEAK, true);
//            }
//        }
        if (MovementHelper.isLiquid(dest.above())) {
            return true;
        }
        return super.prepared(state);
    }
}
