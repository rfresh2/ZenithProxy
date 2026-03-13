package com.zenith.feature.pathfinder.movement.movements;

import com.google.common.collect.ImmutableSet;
import com.zenith.feature.pathfinder.BlockStateInterface;
import com.zenith.feature.pathfinder.PathInput;
import com.zenith.feature.pathfinder.movement.*;
import com.zenith.feature.pathfinder.movement.MovementHelper.PlaceResult;
import com.zenith.feature.pathfinder.util.RotationUtils;
import com.zenith.feature.pathfinder.util.VecUtils;
import com.zenith.feature.player.Rotation;
import com.zenith.feature.player.RotationHelper;
import com.zenith.feature.player.World;
import com.zenith.mc.block.Block;
import com.zenith.mc.block.BlockPos;
import com.zenith.mc.block.BlockRegistry;
import com.zenith.mc.block.BlockTags;
import lombok.ToString;
import org.cloudburstmc.math.vector.Vector3d;

import java.util.Optional;
import java.util.Set;

import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.PATH_LOG;
import static com.zenith.feature.pathfinder.movement.ActionCosts.*;

@ToString(callSuper = true)
public class MovementTraverse extends Movement {

    /**
     * Did we have to place a bridge block or was it always there
     */
    private boolean wasTheBridgeBlockAlwaysThere = true;

    public MovementTraverse(BlockPos from, BlockPos to) {
        super(from, to, new BlockPos[]{to.above(), to}, to.below());
    }

    @Override
    public void reset() {
        super.reset();
        wasTheBridgeBlockAlwaysThere = true;
    }

    @Override
    public double calculateCost(CalculationContext context) {
        return cost(context, src.x(), src.y(), src.z(), dest.x(), dest.z());
    }

    @Override
    protected Set<BlockPos> calculateValidPositions() {
        return ImmutableSet.of(src, dest); // src.above means that we don't get caught in an infinite loop in water
    }

    public static double cost(CalculationContext context, int x, int y, int z, int destX, int destZ) {
        int pb0 = context.getId(destX, y + 1, destZ);
        Block pb0Block = BlockStateInterface.getBlock(pb0);
        int pb1 = context.getId(destX, y, destZ);
        Block pb1Block = BlockStateInterface.getBlock(pb1);
        int destOn = context.getId(destX, y - 1, destZ);
        Block destOnBlock = BlockStateInterface.getBlock(destOn);
        int srcDown = context.getId(x, y - 1, z);
        Block srcDownBlock = BlockStateInterface.getBlock(srcDown);
        boolean standingOnABlock = MovementHelper.mustBeSolidToWalkOn(context, x, y - 1, z, srcDown);
        boolean frostWalker = false; // standingOnABlock && !context.assumeWalkOnWater && MovementHelper.canUseFrostWalker(context, destOn);
        if (frostWalker || MovementHelper.canWalkOn(context, destX, y - 1, destZ, destOn)) { //this is a walk, not a bridge
            double WC = WALK_ONE_BLOCK_COST;
            boolean liquid = false;
            if (MovementHelper.isWater(pb0Block) || MovementHelper.isWater(pb1Block)) {
                WC = context.waterWalkSpeed;
                liquid = true;
            } else if (MovementHelper.isLava(pb0Block) || MovementHelper.isLava(pb1Block)) {
                WC = context.lavaWalkSpeed;
                liquid = true;
            } else {
                if (destOnBlock == BlockRegistry.SOUL_SAND) {
                    WC += (WALK_ONE_OVER_SOUL_SAND_COST - WALK_ONE_BLOCK_COST) / 2;
                } else if (frostWalker) {
                    // with frostwalker we can walk on water without the penalty, if we are sure we won't be using jesus
                } else if (destOnBlock == BlockRegistry.WATER) {
                    WC += context.walkOnWaterOnePenalty;
                }
                if (srcDownBlock == BlockRegistry.SOUL_SAND) {
                    WC += (WALK_ONE_OVER_SOUL_SAND_COST - WALK_ONE_BLOCK_COST) / 2;
                }
            }
            double hardness1 = MovementHelper.getMiningDurationTicks(context, destX, y, destZ, pb1, false);
            if (hardness1 >= COST_INF) {
                return COST_INF;
            }
            double hardness2 = MovementHelper.getMiningDurationTicks(context, destX, y + 1, destZ, pb0, true); // only include falling on the upper block to break
            if (hardness1 == 0 && hardness2 == 0) {
                if (!liquid && context.canSprint) {
                    // If there's nothing in the way, and this isn't water, and we aren't sneak placing
                    // We can sprint =D
                    // Don't check for soul sand, since we can sprint on that too
                    WC *= SPRINT_MULTIPLIER;
                }
                return WC;
            }
            if (srcDownBlock.blockTags().contains(BlockTags.CLIMBABLE)) {
                hardness1 *= 5;
                hardness2 *= 5;
            }
            return WC + hardness1 + hardness2;
        } else {//this is a bridge, so we need to place a block
            if (srcDownBlock.blockTags().contains(BlockTags.CLIMBABLE)) {
                return COST_INF;
            }
            if (MovementHelper.isReplaceable(destX, destZ, destOn)) {
                boolean throughWater = MovementHelper.isWater(pb0Block) || MovementHelper.isWater(pb1Block);
                boolean throughLava = MovementHelper.isLava(pb0Block) || MovementHelper.isLava(pb1Block);
                if (MovementHelper.isWater(destOnBlock) && throughWater) {
                    // this happens when assume walk on water is true and this is a traverse in water, which isn't allowed
                    return COST_INF;
                }
                double placeCost = context.costOfPlacingAt(context, destX, y - 1, destZ, destOn);
                if (placeCost >= COST_INF) {
                    return COST_INF;
                }
                double hardness1 = MovementHelper.getMiningDurationTicks(context, destX, y, destZ, pb1, false);
                if (hardness1 >= COST_INF) {
                    return COST_INF;
                }
                double hardness2 = MovementHelper.getMiningDurationTicks(context, destX, y + 1, destZ, pb0, true); // only include falling on the upper block to break
                double WC = throughWater ? context.waterWalkSpeed : throughLava ? context.lavaWalkSpeed : WALK_ONE_BLOCK_COST;
                for (int i = 0; i < 5; i++) {
                    int againstX = destX + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[i].x();
                    int againstY = y - 1 + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[i].y();
                    int againstZ = destZ + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[i].z();
                    if (againstX == x && againstZ == z) { // this would be a backplace
                        continue;
                    }
                    if (MovementHelper.canPlaceAgainst(againstX, againstY, againstZ)) { // found a side place option
                        return WC + placeCost + hardness1 + hardness2;
                    }
                }
                // now that we've checked all possible directions to side place, we actually need to backplace
                if (srcDownBlock == BlockRegistry.SOUL_SAND || (srcDownBlock.blockTags().contains(BlockTags.SLABS) && !BlockStateInterface.isDoubleSlab(srcDown))) {
                    return COST_INF; // can't sneak and backplace against soul sand or half slabs (regardless of whether it's top half or bottom half) =/
                }
                if (!standingOnABlock) { // standing on water / swimming
                    return COST_INF; // this is obviously impossible
                }
                Block blockSrc = context.getBlock(x, y, z);
                if ((blockSrc == BlockRegistry.LILY_PAD || blockSrc.blockTags().contains(BlockTags.WOOL_CARPETS)) && World.isFluid(srcDownBlock)) {
                    return COST_INF; // we can stand on these but can't place against them
                }
                WC = WC * (SNEAK_ONE_BLOCK_COST / WALK_ONE_BLOCK_COST);//since we are sneak backplacing, we are sneaking lol
                return WC + placeCost + hardness1 + hardness2;
            }
            return COST_INF;
        }
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        int pb0 = BlockStateInterface.getId(positionsToBreak[0]);
        Block pb0Block = BlockStateInterface.getBlock(pb0);
        int pb1 = BlockStateInterface.getId(positionsToBreak[1]);
        Block pb1Block = BlockStateInterface.getBlock(pb1);
        if (state.getStatus() != MovementStatus.RUNNING) {
            // if the setting is enabled
//            if (!Baritone.settings().walkWhileBreaking.value) {
//                return state;
//            }
            // and if we're prepping (aka mining the block in front)
            if (state.getStatus() != MovementStatus.PREPPING) {
                return state;
            }
            // and if it's fine to walk into the blocks in front
            if (MovementHelper.avoidWalkingInto(pb0Block)) {
                return state;
            }
            if (MovementHelper.avoidWalkingInto(pb1Block)) {
                return state;
            }
            // and we aren't already pressed up against the block
            double dist = Math.max(Math.abs(ctx.player().getX() - (dest.x() + 0.5D)), Math.abs(ctx.player().getZ() - (dest.z() + 0.5D)));
            if (dist < 0.83) {
                return state;
            }
            if (state.getTarget().rotation() == null) {
                // this can happen rarely when the server lags and doesn't send the falling sand entity until you've already walked through the block and are now mining the next one
                return state;
            }

            // combine the yaw to the center of the destination, and the pitch to the specific block we're trying to break
            // it's safe to do this since the two blocks we break (in a traverse) are right on top of each other and so will have the same yaw
            Vector3d blockCenter = VecUtils.calculateBlockCenter(dest);

            float yawToDest = RotationHelper.rotationTo(blockCenter.getX(), blockCenter.getY(), blockCenter.getZ()).getX(); // RotationUtils.calcRotationFromVec3d(ctx.playerHead(), VecUtils.calculateBlockCenter(dest), ctx.playerRotations()).getYaw();
            float pitchToBreak = state.getTarget().rotation().pitch();
            if ((MovementHelper.isBlockNormalCube(pb0) || pb0Block.isAir() && (MovementHelper.isBlockNormalCube(pb1) || pb1Block.isAir()))) {
                // in the meantime, before we're right up against the block, we can break efficiently at this angle
                pitchToBreak = 26;
            }

            return state.setTarget(new MovementState.MovementTarget(new Rotation(yawToDest, pitchToBreak), true))
                .setInput(PathInput.MOVE_FORWARD, true)
                .setInput(PathInput.SPRINT, true);
        }

        //sneak may have been set to true in the PREPPING state while mining an adjacent block
        state.setInput(PathInput.SNEAK, false);

        Block fd = BlockStateInterface.getBlock(src.below());
        boolean ladder = fd.blockTags().contains(BlockTags.CLIMBABLE);

        if (pb0Block.blockTags().contains(BlockTags.DOORS) || pb1Block.blockTags().contains(BlockTags.DOORS)) {
            boolean notPassable = pb0Block.blockTags().contains(BlockTags.DOORS) && !MovementHelper.isDoorPassable(src, dest) || pb1Block.blockTags().contains(BlockTags.DOORS) && !MovementHelper.isDoorPassable(dest, src);
            boolean canOpen = !(BlockRegistry.IRON_DOOR.equals(pb0Block) || BlockRegistry.IRON_DOOR.equals(pb1Block));

            if (notPassable && canOpen) {
                return state.setTarget(new MovementState.MovementTarget(RotationUtils.calcRotationFromVec3d(ctx.playerHead(), VecUtils.calculateBlockCenter(positionsToBreak[0]), ctx.playerRotations()), true))
                    .setInput(PathInput.RIGHT_CLICK_BLOCK, true)
                    .setClickTarget(positionsToBreak[0]);
            }
        }

        var src0Block = BlockStateInterface.getBlock(src);
        var src1Block = BlockStateInterface.getBlock(src.above());
        if (src0Block.blockTags().contains(BlockTags.DOORS) || src1Block.blockTags().contains(BlockTags.DOORS)) {
            boolean notPassable = src0Block.blockTags().contains(BlockTags.DOORS) && !MovementHelper.isDoorPassable(src, dest) || src1Block.blockTags().contains(BlockTags.DOORS) && !MovementHelper.isDoorPassable(dest, src);
            boolean canOpen = !(BlockRegistry.IRON_DOOR.equals(src0Block) || BlockRegistry.IRON_DOOR.equals(src1Block));
            if (notPassable && canOpen) {
                return state.setTarget(new MovementState.MovementTarget(RotationUtils.calcRotationFromVec3d(ctx.playerHead(), VecUtils.calculateBlockCenter(src), ctx.playerRotations()), true))
                    .setInput(PathInput.RIGHT_CLICK_BLOCK, true)
                    .setClickTarget(src);
            }
        }

        if (pb0Block.blockTags().contains(BlockTags.FENCE_GATES) || pb1Block.blockTags().contains(BlockTags.FENCE_GATES)) {
            BlockPos blocked = !MovementHelper.isGatePassable(positionsToBreak[0], src.above()) ? positionsToBreak[0]
                : !MovementHelper.isGatePassable(positionsToBreak[1], src) ? positionsToBreak[1]
                : null;
            if (blocked != null) {
                Optional<Rotation> rotation = RotationUtils.reachable(ctx, blocked);
                if (rotation.isPresent()) {
                    return state.setTarget(new MovementState.MovementTarget(rotation.get(), true))
                        .setInput(PathInput.RIGHT_CLICK_BLOCK, true)
                        .setClickTarget(blocked);
                }
            }
        }

        boolean isTheBridgeBlockThere = MovementHelper.canWalkOn(positionToPlace) || ladder || MovementHelper.canUseFrostWalker(ctx, positionToPlace);
        BlockPos feet = ctx.playerFeet();
        if (feet.y() != dest.y() && !ladder) {
            PATH_LOG.debug("Wrong Y coordinate");
            if (feet.y() < dest.y()) {
                return state.setInput(PathInput.JUMP, true);
            }
            return state;
        }

        if (isTheBridgeBlockThere) {
            if (feet.equals(dest)) {
                return state.setStatus(MovementStatus.SUCCESS);
            }
            if (feet.equals(dest.offset(getDirection())) || feet.equals(dest.offset(getDirection()).offset(getDirection()))) {
                return state.setStatus(MovementStatus.SUCCESS);
            }
            Block low = BlockStateInterface.getBlock(src);
            Block high = BlockStateInterface.getBlock(src.above());
            if (ctx.player().getY() > src.y() + 0.1D
                && !ctx.player().isOnGround()
                && (low.blockTags().contains(BlockTags.CLIMBABLE) || high.blockTags().contains(BlockTags.CLIMBABLE))
                && !BlockStateInterface.getBlock(src.below()).blockTags().contains(BlockTags.CLIMBABLE)) {
                // hitting W could cause us to climb the ladder instead of going forward
                // wait until we're on the ground
                return state;
            }
            BlockPos into = dest.subtract(src).offset(dest);
            Block intoBelowBlock = BlockStateInterface.getBlock(into);
            Block intoAboveBlock = BlockStateInterface.getBlock(into.above());
            if (wasTheBridgeBlockAlwaysThere && (!MovementHelper.avoidWalkingInto(intoBelowBlock) || MovementHelper.isWater(intoBelowBlock)) && !MovementHelper.avoidWalkingInto(intoAboveBlock)) {
                state.setInput(PathInput.SPRINT, true);
            }

            if (low.blockTags().contains(BlockTags.CLIMBABLE)) {
                state.setInput(PathInput.JUMP, true);
            }
            BlockPos against = positionsToBreak[0];
            MovementHelper.moveTowards(state, against);
            if (CONFIG.client.extra.pathfinder.traverseCentering) {
                MovementHelper.centerSideways(state, ctx, src, dest);
            }
            return state;
        } else {
            wasTheBridgeBlockAlwaysThere = false;
            Block standingOn = BlockStateInterface.getBlock(feet.below());
            if (standingOn == BlockRegistry.SOUL_SAND || standingOn.blockTags().contains(BlockTags.SLABS)) { // see issue #118
                double dist = Math.max(Math.abs(dest.x() + 0.5 - ctx.player().getX()), Math.abs(dest.z() + 0.5 - ctx.player().getZ()));
                if (dist < 0.85) { // 0.5 + 0.3 + epsilon
                    MovementHelper.moveTowards(state, dest);
                    return state.setInput(PathInput.MOVE_FORWARD, false)
                        .setInput(PathInput.MOVE_BACK, true);
                }
            }
            double dist1 = Math.max(Math.abs(ctx.player().getX() - (dest.x() + 0.5D)), Math.abs(ctx.player().getZ() - (dest.z() + 0.5D)));
            PlaceResult p = MovementHelper.attemptToPlaceABlock(state, dest.below(), false, true);
            if ((p == PlaceResult.READY_TO_PLACE || dist1 < 0.6)) {
                state.setInput(PathInput.SNEAK, true);
            }
            PATH_LOG.debug("PlaceResult: {}", p);
            switch (p) {
                case READY_TO_PLACE -> {
                    if (ctx.player().isSneaking()) {
                        state.setClickTarget(ctx.getSelectedBlock().orElse(null));
                        state.setInput(PathInput.RIGHT_CLICK_BLOCK, true);
                    }
                    return state;
                }
                case ATTEMPTING -> {
                    if (dist1 > 0.83) {
                        // might need to go forward a bit
                        float yaw = RotationUtils.calcRotationFromVec3d(ctx.playerHead(),
                            VecUtils.getBlockPosCenter(dest),
                            ctx.playerRotations()).yaw();
                        if (Math.abs(state.getTarget().rotation().yaw() - yaw) < 0.1) {
                            // but only if our attempted place is straight ahead
                            return state.setInput(PathInput.MOVE_FORWARD, true);
                        }
                    } else if (ctx.playerRotations().isReallyCloseTo(state.getTarget().rotation())) {
                        // well i guess theres something in the way
                        // todo: set input click target?
                        return state.setInput(PathInput.LEFT_CLICK_BLOCK, true);
                    }
                    return state;
                }
                default -> {}
            }
            if (feet.equals(dest)) {
                // If we are in the block that we are trying to get to, we are sneaking over air and we need to place a block beneath us against the one we just walked off of
                // Out.log(from + " " + to + " " + faceX + "," + faceY + "," + faceZ + " " + whereAmI);
                double faceX = (dest.x() + src.x() + 1.0D) * 0.5D;
                double faceY = (dest.y() + src.y() - 1.0D) * 0.5D;
                double faceZ = (dest.z() + src.z() + 1.0D) * 0.5D;
                // faceX, faceY, faceZ is the middle of the face between from and to
                BlockPos goalLook = src.below(); // this is the block we were just standing on, and the one we want to place against

                Rotation backToFace = RotationUtils.calcRotationFromVec3d(ctx.playerHead(), Vector3d.from(faceX, faceY, faceZ), ctx.playerRotations());
                float pitch = backToFace.pitch();
                double dist2 = Math.max(Math.abs(ctx.player().getX() - faceX), Math.abs(ctx.player().getZ() - faceZ));
                if (dist2 < 0.29) { // see issue #208
                    float yaw = RotationUtils.calcRotationFromVec3d(VecUtils.getBlockPosCenter(dest), ctx.playerHead(), ctx.playerRotations()).yaw();
                    state.setTarget(new MovementState.MovementTarget(new Rotation(yaw, pitch), true));
                    state.setInput(PathInput.MOVE_BACK, true);
                } else {
                    state.setTarget(new MovementState.MovementTarget(backToFace, true));
                }
                if (ctx.isLookingAt(goalLook)) {
                    state.setClickTarget(goalLook);
                    return state.setInput(PathInput.RIGHT_CLICK_BLOCK, true); // wait to right click until we are able to place
                }
                // Out.log("Trying to look at " + goalLook + ", actually looking at" + Baritone.whatAreYouLookingAt());
                if (ctx.playerRotations().isReallyCloseTo(state.getTarget().rotation())) {
                    // todo: set click target?
                    state.setInput(PathInput.LEFT_CLICK_BLOCK, true);
                }
                return state;
            }
            MovementHelper.moveTowards(state, positionsToBreak[0]);
            return state;
            // TODO MovementManager.moveTowardsBlock(to); // move towards not look at because if we are bridging for a couple blocks in a row, it is faster if we dont spin around and walk forwards then spin around and place backwards for every block
        }
    }

    @Override
    public boolean safeToCancel(MovementState state) {
        // if we're in the process of breaking blocks before walking forwards
        // or if this isn't a sneak place (the block is already there)
        // then it's safe to cancel this
        return state.getStatus() != MovementStatus.RUNNING || MovementHelper.canWalkOn(dest.below());
    }

    @Override
    protected boolean prepared(MovementState state) {
        if (ctx.playerFeet().equals(src) || ctx.playerFeet().equals(src.below())) {
            Block block = BlockStateInterface.getBlock(src.below());
            if (block.blockTags().contains(BlockTags.CLIMBABLE)) {
                state.setInput(PathInput.SNEAK, true);
            }
        }
        return super.prepared(state);
    }
}
