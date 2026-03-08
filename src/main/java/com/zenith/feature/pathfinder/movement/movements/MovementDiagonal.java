package com.zenith.feature.pathfinder.movement.movements;

import com.google.common.collect.ImmutableSet;
import com.zenith.feature.pathfinder.BlockStateInterface;
import com.zenith.feature.pathfinder.MutableMoveResult;
import com.zenith.feature.pathfinder.PathInput;
import com.zenith.feature.pathfinder.movement.*;
import com.zenith.feature.player.Bot;
import com.zenith.feature.player.World;
import com.zenith.mc.block.*;
import com.zenith.util.math.MathHelper;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.zenith.Globals.CONFIG;
import static com.zenith.feature.pathfinder.movement.ActionCosts.*;

@ToString(callSuper = true)
public class MovementDiagonal extends Movement {

    private static final double SQRT_2 = Math.sqrt(2);

    public MovementDiagonal(BlockPos start, Direction dir1, Direction dir2, int dy) {
        this(start, start.relative(dir1), start.relative(dir2), dir2, dy);
    }

    private MovementDiagonal(BlockPos start, BlockPos dir1, BlockPos dir2, Direction drr2, int dy) {
        this(start, dir1.add(drr2.x(), drr2.y() + dy, drr2.z()), dir1, dir2);
    }

    private MovementDiagonal(BlockPos start, BlockPos end, BlockPos dir1, BlockPos dir2) {
        super(start, end, new BlockPos[]{dir1, dir1.above(), dir2, dir2.above(), end, end.above()});
    }

    @Override
    protected boolean safeToCancel(MovementState state) {
        //too simple. backfill does not work after cornering with this
        //return context.precomputedData.canWalkOn(ctx, ctx.playerFeet().down());
        Bot player = ctx.player();
        double offset = 0.25;
        double x = player.getX();
        double y = player.getY() - 1;
        double z = player.getZ();
        //standard
        if (ctx.playerFeet().equals(src)) {
            return true;
        }
        //both corners are walkable
        if (MovementHelper.canWalkOn(new BlockPos(src.x(), src.y() - 1, dest.z()))
                && MovementHelper.canWalkOn(new BlockPos(dest.x(), src.y() - 1, src.z()))) {
            return true;
        }
        //we are in a likely unwalkable corner, check for a supporting block
        if (ctx.playerFeet().equals(new BlockPos(src.x(), src.y(), dest.z()))
                || ctx.playerFeet().equals(new BlockPos(dest.x(), src.y(), src.z()))) {
            return (MovementHelper.canWalkOn(new BlockPos(x + offset, y, z + offset))
                    || MovementHelper.canWalkOn(new BlockPos(x + offset, y, z - offset))
                    || MovementHelper.canWalkOn(new BlockPos(x - offset, y, z + offset))
                    || MovementHelper.canWalkOn(new BlockPos(x - offset, y, z - offset)));
        }
        return true;
    }

    @Override
    public double calculateCost(CalculationContext context) {
        MutableMoveResult result = new MutableMoveResult();
        cost(context, src.x(), src.y(), src.z(), dest.x(), dest.z(), result);
        if (result.y != dest.y()) {
            return COST_INF; // doesn't apply to us, this position is incorrect
        }
        return result.cost;
    }

    @Override
    protected Set<BlockPos> calculateValidPositions() {
        BlockPos diagA = new BlockPos(src.x(), src.y(), dest.z());
        BlockPos diagB = new BlockPos(dest.x(), src.y(), src.z());
        if (dest.y() < src.y()) {
            return ImmutableSet.of(src, dest.above(), diagA, diagB, dest, diagA.below(), diagB.below());
        }
        if (dest.y() > src.y()) {
            return ImmutableSet.of(src, src.above(), diagA, diagB, dest, diagA.above(), diagB.above());
        }
        return ImmutableSet.of(src, dest, diagA, diagB);
    }

    public static void cost(CalculationContext context, int x, int y, int z, int destX, int destZ, MutableMoveResult res) {
        if (!MovementHelper.canWalkThrough(context, destX, y + 1, destZ)) {
            return;
        }
        int destInto = context.getId(destX, y, destZ);
        int fromDown;
        boolean ascend = false;
        int destWalkOn;
        boolean descend = false;
        boolean frostWalker = false;
        if (!MovementHelper.canWalkThrough(context, destX, y, destZ, destInto)) {
            ascend = true;
            if (!context.allowDiagonalAscend || !MovementHelper.canWalkThrough(context, x, y + 2, z) || !MovementHelper.canWalkOn(context, destX, y, destZ, destInto) || !MovementHelper.canWalkThrough(context, destX, y + 2, destZ)) {
                return;
            }
            destWalkOn = destInto;
            fromDown = context.getId(x, y - 1, z);
        } else {
            destWalkOn = context.getId(destX, y - 1, destZ);
            fromDown = context.getId(x, y - 1, z);
            boolean standingOnABlock = MovementHelper.mustBeSolidToWalkOn(context, x, y - 1, z, fromDown);
            frostWalker = standingOnABlock && MovementHelper.canUseFrostWalker(context, destWalkOn);
            if (!frostWalker && !MovementHelper.canWalkOn(context, destX, y - 1, destZ, destWalkOn)) {
                descend = true;
                if (!context.allowDiagonalDescend || !MovementHelper.canWalkOn(context, destX, y - 2, destZ) || !MovementHelper.canWalkThrough(context, destX, y - 1, destZ, destWalkOn)) {
                    return;
                }
            }
            frostWalker &= !context.assumeWalkOnWater; // do this after checking for descends because jesus can't prevent the water from freezing, it just prevents us from relying on the water freezing
        }
        double multiplier = WALK_ONE_BLOCK_COST;
        // For either possible soul sand, that affects half of our walking
        Block destWalkOnBlock = BlockStateInterface.getBlock(destWalkOn);
        if (destWalkOnBlock == BlockRegistry.SOUL_SAND) {
            multiplier += (WALK_ONE_OVER_SOUL_SAND_COST - WALK_ONE_BLOCK_COST) / 2;
        } else if (frostWalker) {
            // frostwalker lets us walk on water without the penalty
        } else if (destWalkOnBlock == BlockRegistry.WATER) {
            multiplier += context.walkOnWaterOnePenalty * SQRT_2;
        }
        Block fromDownBlock = BlockStateInterface.getBlock(fromDown);
        if (fromDownBlock == BlockRegistry.LADDER || fromDownBlock == BlockRegistry.VINE) {
            return;
        }
        if (fromDownBlock == BlockRegistry.SOUL_SAND) {
            multiplier += (WALK_ONE_OVER_SOUL_SAND_COST - WALK_ONE_BLOCK_COST) / 2;
        }
        Block cuttingOver1 = context.getBlock(x, y - 1, destZ);
        if (cuttingOver1 == BlockRegistry.MAGMA_BLOCK || MovementHelper.isLava(cuttingOver1)) {
            return;
        }
        Block cuttingOver2 = context.getBlock(destX, y - 1, z);
        if (cuttingOver2 == BlockRegistry.MAGMA_BLOCK || MovementHelper.isLava(cuttingOver2)) {
            return;
        }
        boolean liquid = false;
        Block startIn = context.getBlock(x, y, z);
        if (MovementHelper.isWater(startIn) || MovementHelper.isWater(BlockStateInterface.getBlock(destInto))) {
            if (ascend) {
                return;
            }
            // Ignore previous multiplier
            // Whatever we were walking on (possibly soul sand) doesn't matter as we're actually floating on water
            // Not even touching the blocks below
            multiplier = context.waterWalkSpeed;
            liquid = true;
        } else if (MovementHelper.isLava(startIn) || MovementHelper.isLava(BlockStateInterface.getBlock(destInto))) {
            if (ascend) {
                return;
            }
            multiplier = context.lavaWalkSpeed;
            liquid = true;
        }
        int pb0 = context.getId(x, y, destZ);
        Block pb0Block = BlockStateInterface.getBlock(pb0);
        int pb2 = context.getId(destX, y, z);
        Block pb2Block = BlockStateInterface.getBlock(pb2);
        if (ascend) {
            boolean ATop = MovementHelper.canWalkThrough(context, x, y + 2, destZ);
            boolean AMid = MovementHelper.canWalkThrough(context, x, y + 1, destZ);
            boolean ALow = MovementHelper.canWalkThrough(context, x, y, destZ, pb0);
            boolean BTop = MovementHelper.canWalkThrough(context, destX, y + 2, z);
            boolean BMid = MovementHelper.canWalkThrough(context, destX, y + 1, z);
            boolean BLow = MovementHelper.canWalkThrough(context, destX, y, z, pb2);
            if ((!(ATop && AMid && ALow) && !(BTop && BMid && BLow)) // no option
                    || MovementHelper.avoidWalkingInto(pb0Block) // bad
                    || MovementHelper.avoidWalkingInto(pb2Block) // bad
                    || (ATop && AMid && MovementHelper.canWalkOn(context, x, y, destZ, pb0)) // we could just ascend
                    || (BTop && BMid && MovementHelper.canWalkOn(context, destX, y, z, pb2)) // we could just ascend
                    || (!ATop && AMid && ALow) // head bonk A
                    || (!BTop && BMid && BLow)) { // head bonk B
                return;
            }
            res.cost = multiplier * SQRT_2 + JUMP_ONE_BLOCK_COST;
            res.x = destX;
            res.z = destZ;
            res.y = y + 1;
            return;
        }
        double optionA = MovementHelper.getMiningDurationTicks(context, x, y, destZ, pb0, false);
        double optionB = MovementHelper.getMiningDurationTicks(context, destX, y, z, pb2, false);
        if (optionA != 0 && optionB != 0) {
            // check these one at a time -- if pb0 and pb2 were nonzero, we already know that (optionA != 0 && optionB != 0)
            // so no need to check pb1 as well, might as well return early here
            return;
        }
        int pb1 = context.getId(x, y + 1, destZ);
        Block pb1Block = BlockStateInterface.getBlock(pb1);
        optionA += MovementHelper.getMiningDurationTicks(context, x, y + 1, destZ, pb1, true);
        if (optionA != 0 && optionB != 0) {
            // same deal, if pb1 makes optionA nonzero and option B already was nonzero, pb3 can't affect the result
            return;
        }
        int pb3 = context.getId(destX, y + 1, z);
        Block pb3Block = BlockStateInterface.getBlock(pb3);
        if (optionA == 0 && ((MovementHelper.avoidWalkingInto(pb2Block) && pb2Block != BlockRegistry.WATER) || MovementHelper.avoidWalkingInto(pb3Block))) {
            // at this point we're done calculating optionA, so we can check if it's actually possible to edge around in that direction
            return;
        }
        optionB += MovementHelper.getMiningDurationTicks(context, destX, y + 1, z, pb3, true);
        if (optionA != 0 && optionB != 0) {
            // and finally, if the cost is nonzero for both ways to approach this diagonal, it's not possible
            return;
        }
        if (optionB == 0 && ((MovementHelper.avoidWalkingInto(pb0Block) && pb0Block != BlockRegistry.WATER) || MovementHelper.avoidWalkingInto(pb1Block))) {
            // and now that option B is fully calculated, see if we can edge around that way
            return;
        }
        if (optionA != 0 || optionB != 0) {
            multiplier *= SQRT_2 - 0.001; // TODO tune
            if (startIn == BlockRegistry.LADDER
                || startIn == BlockRegistry.VINE
                || startIn == BlockRegistry.TWISTING_VINES
                || startIn == BlockRegistry.TWISTING_VINES_PLANT
                || startIn == BlockRegistry.WEEPING_VINES
                || startIn == BlockRegistry.WEEPING_VINES_PLANT
            ) {
                // edging around doesn't work if doing so would climb a ladder or vine instead of moving sideways
                return;
            }
        } else {
            // only can sprint if not edging around
            if (context.canSprint && !liquid) {
                // If we aren't edging around anything, and we aren't in water
                // We can sprint =D
                // Don't check for soul sand, since we can sprint on that too
                multiplier *= SPRINT_MULTIPLIER;
            }
        }
        res.cost = multiplier * SQRT_2;
        if (descend) {
            res.cost += Math.max(FALL_N_BLOCKS_COST[1], CENTER_AFTER_FALL_COST);
            res.y = y - 1;
        } else {
            res.y = y;
        }
        res.x = destX;
        res.z = destZ;
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        if (ctx.playerFeet().equals(dest)) {
            return state.setStatus(MovementStatus.SUCCESS);
        } else if (!playerInValidPosition() && !(MovementHelper.isLiquid(src) && getValidPositions().contains(ctx.playerFeet().above()))) {
            return state.setStatus(MovementStatus.UNREACHABLE);
        }
        MovementHelper.moveTowards(state, dest);
        if (CONFIG.client.extra.pathfinder.diagonalCentering) {
            MovementHelper.centerSideways(state, ctx, src, dest);
        }

        if (dest.y() > src.y()
            && ctx.player().getY() < src.y() + 0.1
            && ctx.player().isOnGround()
        ) {
            int srcToDestXSign = MathHelper.sign(dest.x() - src.x());
            int srcToDestZSign = MathHelper.sign(dest.z() - src.z());
            BlockPos feetLevelBlockingBlockX = new BlockPos(src.x() + srcToDestXSign, src.y(), src.z());
            BlockPos feetLevelBlockingBlockZ = new BlockPos(src.x(), src.y(), src.z() + srcToDestZSign);
            boolean feetLevelBlockingBlockPresent = !MovementHelper.canWalkThrough(feetLevelBlockingBlockX) || !MovementHelper.canWalkThrough(feetLevelBlockingBlockZ);

            BlockPos headLevelBlockingBlockX = feetLevelBlockingBlockX.above();
            BlockPos headLevelBlockingBlockZ = feetLevelBlockingBlockZ.above();
            boolean headLevelBlockingBlockPresent = !MovementHelper.canWalkThrough(headLevelBlockingBlockX) || !MovementHelper.canWalkThrough(headLevelBlockingBlockZ);

            BlockPos headLevel2BlockingBlockX = headLevelBlockingBlockX.above();
            BlockPos headLevel2BlockingBlockZ = headLevelBlockingBlockZ.above();
            boolean headLevel2BlockingBlockPresent = !MovementHelper.canWalkThrough(headLevel2BlockingBlockX) || !MovementHelper.canWalkThrough(headLevel2BlockingBlockZ);
            if (!feetLevelBlockingBlockPresent && !headLevelBlockingBlockPresent && !headLevel2BlockingBlockPresent) {
                MovementHelper.safeJump(state, ctx);
            } else if (!headLevelBlockingBlockPresent && !headLevel2BlockingBlockPresent) {
                if (ctx.player().isHorizontalCollision()) {
                    MovementHelper.safeJump(state, ctx);
                }
            } else {
                LocalizedCollisionBox playerMoveCb = ctx.player()
                    .getPlayerCollisionBox()
                    .stretch(srcToDestXSign * 0.1, 0, srcToDestZSign * 0.1);
                List<LocalizedCollisionBox> destCbs = World.getBlockState(dest.below())
                    .getLocalizedCollisionBoxes();
                if (destCbs.stream().anyMatch(cb -> cb.intersects(playerMoveCb))
                    && !ctx.playerFeet().equals(src)) {
                    MovementHelper.safeJump(state, ctx);
                }
            }
        }
        if (sprint() && !state.isInputForced(PathInput.JUMP)) {
            state.setInput(PathInput.SPRINT, true);
        }
        return state;
    }

    private boolean sprint() {
        if (MovementHelper.isLiquid(ctx.playerFeet())) {
            return false;
        }
        for (int i = 0; i < 4; i++) {
            if (!MovementHelper.canWalkThrough(positionsToBreak[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean prepared(MovementState state) {
        return true;
    }

    @Override
    public List<BlockPos> toBreak() {
        if (toBreakCached != null) {
            return toBreakCached;
        }
        List<BlockPos> result = new ArrayList<>();
        for (int i = 4; i < 6; i++) {
            if (!MovementHelper.canWalkThrough(positionsToBreak[i].x(), positionsToBreak[i].y(), positionsToBreak[i].z())) {
                result.add(positionsToBreak[i]);
            }
        }
        toBreakCached = result;
        return result;
    }

    @Override
    public List<BlockPos> toWalkInto() {
        if (toWalkIntoCached == null) {
            toWalkIntoCached = new ArrayList<>();
        }
        List<BlockPos> result = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (!MovementHelper.canWalkThrough(positionsToBreak[i].x(), positionsToBreak[i].y(), positionsToBreak[i].z())) {
                result.add(positionsToBreak[i]);
            }
        }
        toWalkIntoCached = result;
        return toWalkIntoCached;
    }
}
