package com.zenith.feature.pathfinder.movement.movements;

import com.zenith.feature.pathfinder.BlockStateInterface;
import com.zenith.feature.pathfinder.MutableMoveResult;
import com.zenith.feature.pathfinder.PathInput;
import com.zenith.feature.pathfinder.movement.*;
import com.zenith.feature.pathfinder.movement.MovementHelper.PlaceResult;
import com.zenith.feature.player.World;
import com.zenith.mc.block.*;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

import static com.zenith.Globals.BARITONE;
import static com.zenith.Globals.CONFIG;
import static com.zenith.feature.pathfinder.movement.ActionCosts.*;

@ToString(callSuper = true)
public class MovementParkour extends Movement {

    private static final BlockPos[] EMPTY = new BlockPos[]{};

    private final Direction direction;
    private final int dist;
    private final boolean ascend;

    private MovementParkour(BlockPos src, int dist, Direction dir, boolean ascend) {
        super(src, src.relative(dir, dist).above(ascend ? 1 : 0), EMPTY, src.relative(dir, dist).below(ascend ? 0 : 1));
        this.direction = dir;
        this.dist = dist;
        this.ascend = ascend;
    }

    public static MovementParkour cost(CalculationContext context, BlockPos src, Direction direction) {
        MutableMoveResult res = new MutableMoveResult();
        cost(context, src.x(), src.y(), src.z(), direction, res);
        int dist = Math.abs(res.x - src.x()) + Math.abs(res.z - src.z());
        return new MovementParkour(src, dist, direction, res.y > src.y());
    }

    public static void cost(CalculationContext context, int x, int y, int z, Direction dir, MutableMoveResult res) {
        if (!context.allowParkour) {
            return;
        }

        int xDiff = dir.x();
        int zDiff = dir.z();
        if (!MovementHelper.fullyPassable(context, x + xDiff, y, z + zDiff)) {
            // most common case at the top -- the adjacent block isn't air
            return;
        }
        int adj = context.getId(x + xDiff, y - 1, z + zDiff);
        Block adjBlock = BlockStateInterface.getBlock(adj);
        if (MovementHelper.canWalkOn(context, x + xDiff, y - 1, z + zDiff, adj)) { // don't parkour if we could just traverse (for now)
            // second most common case -- we could just traverse not parkour
            return;
        }
        if (MovementHelper.avoidWalkingInto(adjBlock) && !(MovementHelper.isWater(adjBlock))) { // magma sucks
            return;
        }
        if (!MovementHelper.fullyPassable(context, x + xDiff, y + 1, z + zDiff)) {
            return;
        }
        if (!MovementHelper.fullyPassable(context, x + xDiff, y + 2, z + zDiff)) {
            return;
        }
        if (!MovementHelper.fullyPassable(context, x, y + 2, z)) {
            return;
        }
        int standingOn = context.getId(x, y - 1, z);
        Block standingOnBlock = BlockStateInterface.getBlock(standingOn);
        if (standingOnBlock.blockTags().contains(BlockTags.CLIMBABLE) || standingOnBlock.blockTags().contains(BlockTags.STAIRS) || BlockStateInterface.isBottomSlab(standingOn)) {
            return;
        }
        if (World.isFluid(context.getBlock(x, y, z))) {
            return; // can't jump out of water
        }
        int maxJump;
        if (standingOnBlock == BlockRegistry.SOUL_SAND) {
            maxJump = 2; // 1 block gap
        } else {
            if (context.canSprint) {
                maxJump = 4;
            } else {
                maxJump = 3;
            }
        }

        // check parkour jumps from smallest to largest for obstacles/walls and landing positions
        int verifiedMaxJump = 1; // i - 1 (when i = 2)
        for (int i = 2; i <= maxJump; i++) {
            int destX = x + xDiff * i;
            int destZ = z + zDiff * i;

            // check head/feet
            if (!MovementHelper.fullyPassable(context, destX, y + 1, destZ)) {
                break;
            }
            if (!MovementHelper.fullyPassable(context, destX, y + 2, destZ)) {
                break;
            }

            // check for ascend landing position
            int destInto = context.getId(destX, y, destZ);
            if (!MovementHelper.fullyPassable(context, destX, y, destZ, destInto)) {
                if (i <= 3 && context.allowParkourAscend && context.canSprint && MovementHelper.canWalkOn(context, destX, y, destZ, destInto) && checkOvershootSafety(
                    destX + xDiff, y + 1, destZ + zDiff)) {
                    res.x = destX;
                    res.y = y + 1;
                    res.z = destZ;
                    res.cost = i * SPRINT_ONE_BLOCK_COST + context.jumpPenalty;
                    return;
                }
                break;
            }

            // check for flat landing position
            int landingOn = context.getId(destX, y - 1, destZ);
            Block landingOnBlock = BlockStateInterface.getBlock(landingOn);
            // farmland needs to be canWalkOn otherwise farm can never work at all, but we want to specifically disallow ending a jump on farmland haha
            // frostwalker works here because we can't jump from possibly unfrozen water
            if ((landingOnBlock != BlockRegistry.FARMLAND && MovementHelper.canWalkOn(context, destX, y - 1, destZ, landingOn))
                    || (Math.min(16, context.frostWalker + 2) >= i && MovementHelper.canUseFrostWalker(context, landingOn))
            ) {
                if (checkOvershootSafety(destX + xDiff, y, destZ + zDiff)) {
                    res.x = destX;
                    res.y = y;
                    res.z = destZ;
                    res.cost = costFromJumpDistance(i) + context.jumpPenalty;
                    return;
                }
                break;
            }

            if (!MovementHelper.fullyPassable(context, destX, y + 3, destZ)) {
                break;
            }

            verifiedMaxJump = i;
        }

        // parkour place starts here
        if (!context.allowParkourPlace) {
            return;
        }
        // check parkour jumps from largest to smallest for positions to place blocks
        for (int i = verifiedMaxJump; i > 1; i--) {
            int destX = x + i * xDiff;
            int destZ = z + i * zDiff;
            int toReplace = context.getId(destX, y - 1, destZ);
            double placeCost = context.costOfPlacingAt(context, destX, y - 1, destZ, toReplace);
            if (placeCost >= COST_INF) {
                continue;
            }
            if (!MovementHelper.isReplaceable(destX, destZ, toReplace)) {
                continue;
            }
            if (!checkOvershootSafety(destX + xDiff, y, destZ + zDiff)) {
                continue;
            }
            for (int j = 0; j < 5; j++) {
                int againstX = destX + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[j].x();
                int againstY = y - 1 + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[j].y();
                int againstZ = destZ + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[j].z();
                if (againstX == destX - xDiff && againstZ == destZ - zDiff) { // we can't turn around that fast
                    continue;
                }
                if (MovementHelper.canPlaceAgainst(againstX, againstY, againstZ)) {
                    res.x = destX;
                    res.y = y;
                    res.z = destZ;
                    res.cost = costFromJumpDistance(i) + placeCost + context.jumpPenalty;
                    return;
                }
            }
        }
    }

    private static boolean checkOvershootSafety(int x, int y, int z) {
        // we're going to walk into these two blocks after the landing of the parkour anyway, so make sure they aren't avoidWalkingInto
        return !MovementHelper.avoidWalkingInto(BlockStateInterface.getBlock(x, y, z)) && !MovementHelper.avoidWalkingInto(
            BlockStateInterface.getBlock(x, y + 1, z));
    }

    private static double costFromJumpDistance(int dist) {
        switch (dist) {
            case 2:
                return WALK_ONE_BLOCK_COST * 2; // IDK LOL
            case 3:
                return WALK_ONE_BLOCK_COST * 3;
            case 4:
                return SPRINT_ONE_BLOCK_COST * 4;
            default:
                throw new IllegalStateException("LOL " + dist);
        }
    }


    @Override
    public double calculateCost(CalculationContext context) {
        MutableMoveResult res = new MutableMoveResult();
        cost(context, src.x(), src.y(), src.z(), direction, res);
        if (res.x != dest.x() || res.y != dest.y() || res.z != dest.z()) {
            return COST_INF;
        }
        return res.cost;
    }

    @Override
    protected Set<BlockPos> calculateValidPositions() {
        Set<BlockPos> set = new HashSet<>();
        for (int i = 0; i <= dist; i++) {
            for (int y = 0; y < 2; y++) {
                set.add(src.relative(direction, i).above(y));
            }
        }
        return set;
    }

    @Override
    public boolean safeToCancel(MovementState state) {
        // once this movement is instantiated, the state is default to PREPPING
        // but once it's ticked for the first time it changes to RUNNING
        // since we don't really know anything about momentum, it suffices to say Parkour can only be canceled on the 0th tick
        return state.getStatus() != MovementStatus.RUNNING;
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }
        if (ctx.playerFeet().y() < src.y()) {
            // we have fallen
//            logDebug("sorry");
            return state.setStatus(MovementStatus.UNREACHABLE);
        }
        if (ctx.playerFeet().equals(dest) || ctx.playerFeet().equals(dest.offset(getDirection().below()))) {
            return state.setStatus(MovementStatus.SUCCESS);
        }
        if (dist >= 4 || (ascend && dist == 3)) {
            state.setInput(PathInput.SPRINT, true);
        }
        MovementHelper.moveTowards(state, dest);
        if (ctx.playerFeet().equals(dest)) {
            Block d = BlockStateInterface.getBlock(dest);
            if (d.blockTags().contains(BlockTags.CLIMBABLE)) {
                // it physically hurt me to add support for parkour jumping onto a vine
                // but i did it anyway
                return state.setStatus(MovementStatus.SUCCESS);
            }
            if (ctx.player().getY() - ctx.playerFeet().y() < 0.094) { // lilypads
                state.setStatus(MovementStatus.SUCCESS);
            }
        } else if (!ctx.playerFeet().equals(src)) {
            if (ctx.playerFeet().equals(src.relative(direction)) || ctx.player().getY() - src.y() > 0.0001) {
                if (CONFIG.client.extra.pathfinder.allowPlace // see PR #3775
                        && BARITONE.getInventoryBehavior().hasGenericThrowaway()
                        && !MovementHelper.canWalkOn(dest.below())
                        && !ctx.player().isOnGround()
                        && MovementHelper.attemptToPlaceABlock(state, dest.below(), true, false) == PlaceResult.READY_TO_PLACE
                ) {
                    // go in the opposite order to check DOWN before all horizontals -- down is preferable because you don't have to look to the side while in midair, which could mess up the trajectory
                    state.setClickTarget(ctx.getSelectedBlock().orElse(null));
                    state.setInput(PathInput.RIGHT_CLICK_BLOCK, true);
                }
                // prevent jumping too late by checking for ascend
                if (dist == 3 && !ascend) { // this is a 2 block gap, dest = src + direction * 3
                    double xDiff = (src.x() + 0.5) - ctx.player().getX();
                    double zDiff = (src.z() + 0.5) - ctx.player().getZ();
                    double distFromStart = Math.max(Math.abs(xDiff), Math.abs(zDiff));
                    if (distFromStart < 0.7) {
                        return state;
                    }
                }
                if (ctx.player().isOnGround()) {
                    MovementHelper.safeJump(state, ctx);
                }
            } else if (!ctx.playerFeet().equals(dest.relative(direction, -1))) {
                state.setInput(PathInput.SPRINT, false);
                if (ctx.playerFeet().equals(src.relative(direction, -1))) {
                    MovementHelper.moveTowards(state, src);
                } else {
                    MovementHelper.moveTowards(state, src.relative(direction, -1));
                }
            }
        } else if (ctx.playerFeet().equals(src)) {
            MovementHelper.centerSideways(state, ctx, src, dest);
        }
        return state;
    }
}
