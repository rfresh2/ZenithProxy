package com.zenith.feature.pathfinder.movement;

import com.zenith.feature.inventory.InventoryActionRequest;
import com.zenith.feature.inventory.actions.SetHeldItem;
import com.zenith.feature.pathfinder.*;
import com.zenith.feature.pathfinder.movement.MovementState.MovementTarget;
import com.zenith.feature.pathfinder.util.RotationUtils;
import com.zenith.feature.pathfinder.util.ToolSet;
import com.zenith.feature.player.Rotation;
import com.zenith.feature.player.RotationHelper;
import com.zenith.feature.player.World;
import com.zenith.feature.player.raycast.BlockRaycastResult;
import com.zenith.feature.player.raycast.RaycastHelper;
import com.zenith.mc.block.*;
import com.zenith.mc.block.properties.api.BlockStateProperties;
import com.zenith.util.math.MathHelper;
import com.zenith.util.math.MutableVec3d;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.Optional;

import static com.zenith.Globals.*;
import static com.zenith.feature.pathfinder.Ternary.*;
import static com.zenith.feature.pathfinder.movement.ActionCosts.COST_INF;
import static com.zenith.feature.pathfinder.movement.Movement.HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP;
import static java.util.Objects.requireNonNullElse;

public final class MovementHelper {
    private MovementHelper() {}

    public static boolean avoidBreaking(int x, int y, int z, int state) {
//        if (!bsi.worldBorder.canPlaceAt(x, z)) {
//            return true;
//        }
        Block b = BlockStateInterface.getBlock(state);
        return // Baritone.settings().blocksToDisallowBreaking.value.contains(b) ||
            b == BlockRegistry.ICE // ice becomes water, and water can mess up the path
                || b.name().startsWith("infested_") // b instanceof InfestedBlock // obvious reasons
                // call context.get directly with x,y,z. no need to make 5 new BlockPos for no reason
                || avoidAdjacentBreaking(x, y + 1, z, true, false)
                || avoidAdjacentBreaking(x, y - 1, z, false, true)
                || avoidAdjacentBreaking(x + 1, y, z, false, false)
                || avoidAdjacentBreaking(x - 1, y, z, false, false)
                || avoidAdjacentBreaking(x, y, z + 1, false, false)
                || avoidAdjacentBreaking(x, y, z - 1, false, false);
    }

    public static boolean avoidAdjacentBreaking(int x, int y, int z, boolean directlyAbove, boolean directlyBelow) {
        // returns true if you should avoid breaking a block that's adjacent to this one (e.g. lava that will start flowing if you give it a path)
        // this is only called for north, south, east, west, up, and down
        // we assume that it's ALWAYS okay to break the block thats ABOVE liquid
        int state = BlockStateInterface.getId(x, y, z);
        Block block = BlockStateInterface.getBlock(state);

        if (directlyBelow) {
            return block.fallingBlock()
                && CONFIG.client.extra.pathfinder.avoidUpdatingFallingBlocks
                && freeForFallingBlock(x, y - 1, z);
        }

        if (!directlyAbove // it is fine to mine a block that has a falling block directly above, this (the cost of breaking the stacked fallings) is included in cost calculations
            // therefore if directlyAbove is true, we will actually ignore if this is falling
            && block.fallingBlock() // obviously, this check is only valid for falling blocks
            && CONFIG.client.extra.pathfinder.avoidUpdatingFallingBlocks // and if the setting is enabled
            && freeForFallingBlock(x, y - 1, z)) { // and if it would fall (i.e. it's unsupported)
            return true; // dont break a block that is adjacent to unsupported gravel because it can cause really weird stuff
        }
        // only pure liquids for now
        // waterlogged blocks can have closed bottom sides and such
        if (isLiquid(block)) {
            if (directlyAbove) {
                return true;
            }
            FluidState fluidState = World.getFluidState(state);
            if (fluidState != null && fluidState.source()) return true; // source blocks like to flow horizontally

            // everything else will prefer flowing down
            return !isLiquid(BlockStateInterface.getBlock(x, y -1, z)); // assume everything is in a static state
        }
        return World.getFluidState(x, y, z) != null;
    }

    static boolean freeForFallingBlock(int x, int y, int z) {
        var block = BlockStateInterface.getBlock(x, y, z);
        if (block.fallingBlock()) {
            return freeForFallingBlock(x, y - 1, z);
        }
        return block.isAir()
            || block == BlockRegistry.FIRE || block == BlockRegistry.SOUL_FIRE
            || isLiquid(block)
            || block.replaceable();

    }

    public static boolean canWalkThrough(BlockPos pos) {
        return canWalkThrough(pos.x(), pos.y(), pos.z());
    }

    public static boolean canWalkThrough(int x, int y, int z) {
        return canWalkThrough(x, y, z, BlockStateInterface.getId(x, y, z));
    }

    public static boolean canWalkThrough(CalculationContext context, int x, int y, int z, int state) {
        return context.precomputedData.canWalkThrough(x, y, z, state);
    }

    public static boolean canWalkThrough(CalculationContext context, int x, int y, int z) {
        return context.precomputedData.canWalkThrough(x, y, z, context.getId(x, y, z));
    }

    public static boolean canWalkThrough(int x, int y, int z, int blockStateId) {
        return PrecomputedData.INSTANCE.canWalkThrough(x, y, z, blockStateId);
    }

    public static Ternary canWalkThroughBlockState(int blockStateId) {
        Block block = BlockStateInterface.getBlock(blockStateId);
        if (block.isAir()) {
            return YES;
        }
        if (block == BlockRegistry.FIRE
            || block == BlockRegistry.SOUL_FIRE
            || block == BlockRegistry.TRIPWIRE
            || block == BlockRegistry.COBWEB
            || block == BlockRegistry.END_PORTAL
            || block == BlockRegistry.COCOA
            || block.name().endsWith("_skull")
            || block == BlockRegistry.BUBBLE_COLUMN
            || block.blockTags().contains(BlockTags.SHULKER_BOXES)
            || block.blockTags().contains(BlockTags.SLABS)
            || block.blockTags().contains(BlockTags.TRAPDOORS)
            || block == BlockRegistry.HONEY_BLOCK
            || block == BlockRegistry.END_ROD
            || block == BlockRegistry.SWEET_BERRY_BUSH
            || block == BlockRegistry.POINTED_DRIPSTONE
            || block == BlockRegistry.AMETHYST_CLUSTER || block == BlockRegistry.LARGE_AMETHYST_BUD || block == BlockRegistry.MEDIUM_AMETHYST_BUD || block == BlockRegistry.SMALL_AMETHYST_BUD
            || block == BlockRegistry.AZALEA || block == BlockRegistry.FLOWERING_AZALEA
        ) {
            return NO;
        }
        if (block == BlockRegistry.BIG_DRIPLEAF) {
            return NO;
        }
        if (block == BlockRegistry.POWDER_SNOW) {
            return NO;
        }
//        if (Baritone.settings().blocksToAvoid.value.contains(block)) {
//            return NO;
//        }
        if (block.blockTags().contains(BlockTags.DOORS) || block.blockTags().contains(BlockTags.FENCE_GATES)) {
            // TODO this assumes that all doors in all mods are openable
            if (block == BlockRegistry.IRON_DOOR) {
                return NO;
            }
            return YES;
        }
        if (block.blockTags().contains(BlockTags.WOOL_CARPETS)) {
            return MAYBE;
        }
        if (block == BlockRegistry.SNOW) {
            // snow layers cached as the top layer of a packed chunk have no metadata, we can't make a decision based on their depth here
            // it would otherwise make long distance pathing through snowy biomes impossible
            return MAYBE;
        }
        boolean isFluid = World.isFluid(block);
        if (isFluid) {
            FluidState fluidState = World.getFluidState(blockStateId);
            if (fluidState != null) {
                return MAYBE;
            }

        }
        if (block.blockTags().contains(BlockTags.CAULDRONS)) {
            return NO;
        }
        if (BlockStateInterface.isPathfindable(blockStateId)) {
            return YES;
        } else {
            return NO;
        }
    }

    public static boolean canWalkThroughPosition(int x, int y, int z, int blockStateId) {
        Block block = BlockStateInterface.getBlock(blockStateId);

        if (block.blockTags().contains(BlockTags.WOOL_CARPETS)) {
            return canWalkOn(x, y - 1, z);
        }

        if (block == BlockRegistry.SNOW) {
            // if they're cached as a top block, we don't know their metadata
            // default to true (mostly because it would otherwise make long distance pathing through snowy biomes impossible)
            if (!BlockStateInterface.worldContainsLoadedChunk(x, z)) {
                return true;
            }
            // the check in BlockSnow.isPassable is layers < 5
            // while actually, we want < 3 because 3 or greater makes it impassable in a 2 high ceiling
            var layersProperty = World.getBlockStateProperty(blockStateId, BlockStateProperties.LAYERS);
            if (layersProperty != null && layersProperty >= 3) {
                return false;
            }
            // ok, it's low enough we could walk through it, but is it supported?
            return canWalkOn(x, y - 1, z);
        }

        boolean isFluid = World.isFluid(block);
        if (isFluid) {
            var playerInLava = PlayerContext.INSTANCE.player().isTouchingLava() || MovementHelper.isLava(World.getBlock(PlayerContext.INSTANCE.playerFeet()));
            var playerInWater = PlayerContext.INSTANCE.player().isTouchingWater() || MovementHelper.isWater(World.getBlock(PlayerContext.INSTANCE.playerFeet()));
            var playerInFluid = playerInLava || playerInWater;
            if (isFlowing(x, y, z) && !playerInFluid) {
                return false;
            }
            // Everything after this point has to be a special case as it relies on the water not being flowing, which means a special case is needed.
//            if (Baritone.settings().assumeWalkOnWater.value) {
//                return false;
//            }

            Block up = BlockStateInterface.getBlock(x, y + 1, z);
            if (World.isFluid(up) || up == BlockRegistry.LILY_PAD) {
                return false;
            }
            if (isLava(block)) {
                return playerInLava;
            }
            return World.isWater(block);
        }

        return BlockStateInterface.isPathfindable(blockStateId);
    }

    public static boolean fullyPassableBlockState(int state) {
        Block block = BlockStateInterface.getBlock(state);
        if (block.isAir()) { // early return for most common case
            return true;
        }
        // exceptions - blocks that are isPassable true, but we can't actually jump through
        if (block == BlockRegistry.FIRE || block == BlockRegistry.SOUL_FIRE
            || block == BlockRegistry.TRIPWIRE
            || block == BlockRegistry.COBWEB
            || block == BlockRegistry.VINE
            || block == BlockRegistry.LADDER
            || block == BlockRegistry.TWISTING_VINES
            || block == BlockRegistry.TWISTING_VINES_PLANT
            || block == BlockRegistry.WEEPING_VINES
            || block == BlockRegistry.WEEPING_VINES_PLANT
            || block == BlockRegistry.COCOA
            || block == BlockRegistry.AZALEA || block == BlockRegistry.FLOWERING_AZALEA
            || block.blockTags().contains(BlockTags.DOORS)
            || block.blockTags().contains(BlockTags.FENCE_GATES)
            || block == BlockRegistry.SNOW
            || World.isFluid(block)
            || block.blockTags().contains(BlockTags.TRAPDOORS)
            || block == BlockRegistry.END_PORTAL
            || block == BlockRegistry.END_PORTAL_FRAME
            || block.name().endsWith("_skull")
            || block.blockTags().contains(BlockTags.SHULKER_BOXES)
        ) {
            return false;
        }
        // door, fence gate, liquid, trapdoor have been accounted for, nothing else uses the world or pos parameters
        // at least in 1.12.2 vanilla, that is.....
        if (BlockStateInterface.isPathfindable(state)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * canWalkThrough but also won't impede movement at all. so not including doors or fence gates (we'd have to right click),
     * not including water, and not including ladders or vines or cobwebs (they slow us down)
     */
    public static boolean fullyPassable(CalculationContext context, int x, int y, int z) {
        return fullyPassable(context, x, y, z, context.getId(x, y, z));
    }

    public static boolean fullyPassable(CalculationContext context, int x, int y, int z, int blockStateId) {
        return context.precomputedData.fullyPassable(blockStateId);
    }

    public static boolean fullyPassable(BlockPos pos) {
        int state = BlockStateInterface.getId(pos);
        return fullyPassableBlockState(state);
    }

    public static boolean isReplaceable(int x, int z, int blockStateId) {
        // for MovementTraverse and MovementAscend
        // block double plant defaults to true when the block doesn't match, so don't need to check that case
        // all other overrides just return true or false
        // the only case to deal with is snow
        /*
         *  public boolean isReplaceable(IBlockAccess worldIn, BlockPos pos)
         *     {
         *         return ((Integer)worldIn.getBlockState(pos).getValue(LAYERS)).intValue() == 1;
         *     }
         */
        Block block = BlockStateInterface.getBlock(blockStateId);
        if (block.isAir()) {
            // early return for common cases hehe
            return true;
        }
        if (block == BlockRegistry.SNOW) {
            // as before, default to true (mostly because it would otherwise make long distance pathing through snowy biomes impossible)
            if (!BlockStateInterface.worldContainsLoadedChunk(x, z)) {
                return true;
            }
            var layersProperty = World.getBlockStateProperty(blockStateId, BlockStateProperties.LAYERS);
            return layersProperty != null && layersProperty == 1;
        }
        if (block == BlockRegistry.LARGE_FERN || block == BlockRegistry.TALL_GRASS || block == BlockRegistry.SCULK_VEIN) {
            return true;
        }
        return block.replaceable();
    }

    public static boolean isDoorPassable(BlockPos doorPos, BlockPos playerPos) {
        if (playerPos.equals(doorPos)) {
            return false;
        }

        var block = BlockStateInterface.getBlock(doorPos);
        if (!block.blockTags().contains(BlockTags.DOORS)) {
            return true;
        }
        var blockStateId = BlockStateInterface.getId(doorPos);
        var open = World.getBlockStateProperty(block, blockStateId, BlockStateProperties.OPEN);
        if (open == null) {
            return true; // not a door, so we can pass through
        }
        var propertyFacing = World.getBlockStateProperty(block, blockStateId, BlockStateProperties.HORIZONTAL_FACING);
        if (propertyFacing == null) {
            return true; // not a door, so we can pass through
        }
        var facing = propertyFacing.getAxis();

        Direction.Axis playerFacing;
        if (playerPos.north().equals(doorPos) || playerPos.south().equals(doorPos)) {
            playerFacing = Direction.Axis.Z;
        } else if (playerPos.east().equals(doorPos) || playerPos.west().equals(doorPos)) {
            playerFacing = Direction.Axis.X;
        } else {
            return true;
        }

        return (facing == playerFacing) == open;
    }

    public static boolean isGatePassable(BlockPos gatePos, BlockPos playerPos) {
        if (playerPos.equals(gatePos)) {
            return false;
        }

        Block block = BlockStateInterface.getBlock(gatePos);
        int state = BlockStateInterface.getId(gatePos);
        if (!(block.blockTags().contains(BlockTags.FENCE_GATES))) {
            return true;
        }

        var openProperty = World.getBlockStateProperty(block, state, BlockStateProperties.OPEN);
        return requireNonNullElse(openProperty, true);
    }

    public static boolean avoidWalkingInto(Block block) {
        return World.isFluid(block)
            || block == BlockRegistry.MAGMA_BLOCK
            || block == BlockRegistry.CACTUS
            || block == BlockRegistry.SWEET_BERRY_BUSH
            || block == BlockRegistry.FIRE || block == BlockRegistry.SOUL_FIRE
            || block == BlockRegistry.END_PORTAL
            || block == BlockRegistry.COBWEB
            || block == BlockRegistry.BUBBLE_COLUMN;
    }

    /**
     * Can I walk on this block without anything weird happening like me falling
     * through? Includes water because we know that we automatically jump on
     * water
     * <p>
     * If changing something in this function remember to also change it in precomputed data
     *
     * @param x     The block's x position
     * @param y     The block's y position
     * @param z     The block's z position
     * @return Whether or not the specified block can be walked on
     */
    public static boolean canWalkOn(int x, int y, int z, int blockStateId) {
        return PrecomputedData.INSTANCE.canWalkOn(x, y, z, blockStateId);
    }

    public static Ternary canWalkOnBlockState(int blockStateId) {
        Block block = BlockStateInterface.getBlock(blockStateId);
        if (isBlockNormalCube(blockStateId) && block != BlockRegistry.MAGMA_BLOCK && block != BlockRegistry.BUBBLE_COLUMN && block != BlockRegistry.HONEY_BLOCK) {
            return YES;
        }
        if (block == BlockRegistry.AZALEA || block == BlockRegistry.FLOWERING_AZALEA) {
            return YES;
        }
        if (block == BlockRegistry.LADDER
            || block == BlockRegistry.VINE
            || block == BlockRegistry.TWISTING_VINES
            || block == BlockRegistry.TWISTING_VINES_PLANT
            || block == BlockRegistry.WEEPING_VINES
            || block == BlockRegistry.WEEPING_VINES_PLANT)
        { // TODO reconsider this
            return YES;
        }
        if (block == BlockRegistry.FARMLAND || block == BlockRegistry.DIRT_PATH || block == BlockRegistry.SOUL_SAND) {
            return YES;
        }
        if (block == BlockRegistry.ENDER_CHEST || block == BlockRegistry.CHEST || block == BlockRegistry.TRAPPED_CHEST) {
            return YES;
        }
        if (block == BlockRegistry.GLASS || block.name().endsWith("stained_glass")) {
            return YES;
        }
        if (block.blockTags().contains(BlockTags.TRAPDOORS)) {
            var openProperty = World.getBlockStateProperty(blockStateId, BlockStateProperties.OPEN);
            if (openProperty == null || openProperty) {
                return NO;
            }
            return YES;
        }
        if (block.blockTags().contains(BlockTags.STAIRS)) {
            return YES;
        }
        if (isLiquid(block)) {
            return MAYBE;
        }
//        MovementHelper.isLava(block);
        if (block.blockTags().contains(BlockTags.SLABS)) {
            return YES;
        }
        return NO;
    }

    public static boolean canWalkOnPosition(int x, int y, int z, int blockStateId) {
        Block block = BlockStateInterface.getBlock(blockStateId);
        if (isLiquid(block)) {
            var playerInLava = PlayerContext.INSTANCE.player().isTouchingLava() || MovementHelper.isLava(World.getBlock(PlayerContext.INSTANCE.playerFeet()));
            if (isLava(block) && !playerInLava) {
                return false;
            }

            // since this is called literally millions of times per second, the benefit of not allocating millions of useless "pos.up()"
            // BlockPos s that we'd just garbage collect immediately is actually noticeable. I don't even think its a decrease in readability
            int upState = BlockStateInterface.getId(x, y + 1, z);
            Block up = BlockStateInterface.getBlock(upState);
            if (up == BlockRegistry.LILY_PAD || up.name().endsWith("_carpet")) {
                return true;
            }
            if (MovementHelper.isFlowing(x, y, z) || MovementHelper.isFlowing(x, y + 1, z)) {
                // the only scenario in which we can walk on flowing water is if it's under still water with jesus off
                return isLiquid(up);
            }
            // if assumeWalkOnWater is on, we can only walk on water if there isn't water above it
            // if assumeWalkOnWater is off, we can only walk on water if there is water above it
            return isLiquid(up);
        }

//        if (MovementHelper.isLava(state) && !MovementHelper.isFlowing(x, y, z, state, bsi) && Baritone.settings().assumeWalkOnLava.value) { // if we get here it means that assumeWalkOnLava must be true, so put it last
//            return true;
//        }

        return false; // If we don't recognise it then we want to just return false to be safe.
    }

    public static boolean canWalkOn(CalculationContext context, int x, int y, int z, int blockStateId) {
        return context.precomputedData.canWalkOn(x, y, z, blockStateId);
    }

    public static boolean canWalkOn(CalculationContext context, int x, int y, int z) {
        return canWalkOn(context, x, y, z, context.getId(x, y, z));
    }

    public static boolean canWalkOn(BlockPos pos, int state) {
        return canWalkOn(pos.x(), pos.y(), pos.z(), state);
    }

    public static boolean canWalkOn(BlockPos pos) {
        return canWalkOn(pos.x(), pos.y(), pos.z());
    }

    public static boolean canWalkOn(int x, int y, int z) {
        return canWalkOn(x, y, z, BlockStateInterface.getId(x, y, z));
    }

    public static boolean canUseFrostWalker(CalculationContext context, int blockStateId) {
        return false;
//        return context.frostWalker != 0
//            && state == FrostedIceBlock.meltsInto()
//            && state.getValue(LiquidBlock.LEVEL) == 0;
    }

    public static boolean canUseFrostWalker(PlayerContext ctx, BlockPos pos) {
        return false;
//        boolean hasFrostWalker = false;
//        OUTER: for (EquipmentSlot slot : EquipmentSlot.values()) {
//            ItemEnchantments itemEnchantments = ctx
//                .player()
//                .getItemBySlot(slot)
//                .getEnchantments();
//            for (Holder<Enchantment> enchant : itemEnchantments.keySet()) {
//                if (enchant.is(Enchantments.FROST_WALKER)) {
//                    hasFrostWalker = true;
//                    break OUTER;
//                }
//            }
//        }
//        BlockState state = BlockStateInterface.get(ctx, pos);
//        return hasFrostWalker
//            && state == FrostedIceBlock.meltsInto()
//            && state.getValue(LiquidBlock.LEVEL) == 0;
    }

    /**
     * If movements make us stand/walk on this block, will it have a top to walk on?
     */
    public static boolean mustBeSolidToWalkOn(CalculationContext context, int x, int y, int z, int blockStateId) {
        Block block = BlockStateInterface.getBlock(blockStateId);
        if (block.blockTags().contains(BlockTags.CLIMBABLE)) {
            return false;
        }
        if (World.isFluid(block)) { // todo: waterlogged state check
            return false;
            // used for frostwalker so only includes blocks where we are still on ground when leaving them to any side
//            if (block instanceof SlabBlock) {
//                if (state.getValue(SlabBlock.TYPE) != SlabType.BOTTOM) {
//                    return true;
//                }
//            } else if (block instanceof StairBlock) {
//                if (state.getValue(StairBlock.HALF) == Half.TOP) {
//                    return true;
//                }
//                StairsShape shape = state.getValue(StairBlock.SHAPE);
//                if (shape == StairsShape.INNER_LEFT || shape == StairsShape.INNER_RIGHT) {
//                    return true;
//                }
//            } else if (block instanceof TrapDoorBlock) {
//                if (!state.getValue(TrapDoorBlock.OPEN) && state.getValue(TrapDoorBlock.HALF) == Half.TOP) {
//                    return true;
//                }
//            } else if (block == BlockRegistry.SCAFFOLDING) {
//                return true;
//            } else if (block instanceof LeavesBlock) {
//                return true;
//            }
//            if (context.assumeWalkOnWater) {
//                return false;
//            }
//            Block blockAbove = context.getBlock(x, y + 1, z);
//            if (blockAbove instanceof LiquidBlock) {
//                return false;
//            }
        }
        return true;
    }

    public static boolean canPlaceAgainst(int x, int y, int z) {
        return canPlaceAgainst(BlockStateInterface.getId(x, y, z));
    }

    public static boolean canPlaceAgainst(BlockPos pos) {
        return canPlaceAgainst(pos.x(), pos.y(), pos.z());
    }

    public static boolean canPlaceAgainst(int blockStateId) {
//        if (!bsi.worldBorder.canPlaceAt(x, z)) {
//            return false;
//        }
        Block block = BlockStateInterface.getBlock(blockStateId);
        // can we look at the center of a side face of this block and likely be able to place?
        // (thats how this check is used)
        // therefore dont include weird things that we technically could place against (like carpet) but practically can't
        return isBlockNormalCube(blockStateId) || block == BlockRegistry.GLASS || block.name().endsWith("_stained_glass");
    }

    public static double getMiningDurationTicks(CalculationContext context, int x, int y, int z, boolean includeFalling) {
        return getMiningDurationTicks(context, x, y, z, context.getId(x, y, z), includeFalling);
    }

    public static double getMiningDurationTicks(CalculationContext context, int x, int y, int z, int blockStateId, boolean includeFalling) {
        Block block = BlockStateInterface.getBlock(blockStateId);
        if (!canWalkThrough(context, x, y, z, blockStateId)) {
            if (World.isFluid(block)) {
                return COST_INF;
            }
            double mult = context.breakCostMultiplierAt(x, y, z, blockStateId);
            if (mult >= COST_INF) {
                return COST_INF;
            }
            if (avoidBreaking(x, y, z, blockStateId)) {
                return COST_INF;
            }
            double strVsBlock = context.toolSet.getStrVsBlock(block);
            if (strVsBlock <= 0) {
                return COST_INF;
            }
            double result = 1 / strVsBlock;
            result += context.breakBlockAdditionalCost;
            result *= mult;
            if (includeFalling) {
                var above = BlockStateInterface.getBlock(x, y + 1, z);
                if (above.fallingBlock()) {
                    result += getMiningDurationTicks(context, x, y + 1, z, true);
                }
            }
            if (context.goal != null && context.goal.isInGoal(x, y, z)) {
                result = Math.min(result, 20);
            }
            return result;
        }

        return 0; // we won't actually mine it, so don't check fallings above
    }

    /**
     * AutoTool for a specific block
     *
     * @param b   the blockstate to mine
     */
    public static void switchToBestToolFor(Block b) {
        switchToBestToolFor(b, new ToolSet(), CONFIG.client.extra.pathfinder.preferSilkTouch);
    }

    /**
     * AutoTool for a specific block with precomputed ToolSet data
     */
    public static void switchToBestToolFor(Block b, ToolSet ts, boolean preferSilkTouch) {
        if (CONFIG.client.extra.pathfinder.autoTool && !CONFIG.client.extra.pathfinder.assumeExternalAutoTool) {
            int hotbarSlotId = ts.getBestSlot(b, preferSilkTouch);
            INVENTORY.submit(InventoryActionRequest.builder()
                .owner(BARITONE)
                .actions(new SetHeldItem(hotbarSlotId))
                .priority(Baritone.getPriority())
                .build());
        }
    }

    public static void moveTowards(MovementState state, BlockPos pos) {
        var rotVec = RotationHelper.rotationTo(pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);
        var rot = new Rotation(rotVec.getX(), 0);
        state.setTarget(new MovementTarget(rot, false)).setInput(PathInput.MOVE_FORWARD, true);
    }

    public static void moveToBlockCenter(MovementState state, PlayerContext ctx, BlockPos pos) {
        // move to the center of the block
        // also check our velocity will roughly reach 0 xz when we reach the block center

        var rotVec = RotationHelper.rotationTo(pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);
        var rot = new Rotation(rotVec.getX(), 0);
        state.setTarget(new MovementTarget(rot, false));
        MutableVec3d playerVelocity = ctx.player().getVelocity();
        double distanceToBlockCenter = MathHelper.distance2d(ctx.player().getX(), ctx.player().getZ(), pos.x() + 0.5, pos.z() + 0.5);
        if (distanceToBlockCenter < 0.1) {
            // don't move
            return;
        }
        if (Math.abs(playerVelocity.getX()) < 0.03 && Math.abs(playerVelocity.getZ()) < 0.03) {
            state.setInput(PathInput.MOVE_FORWARD, true);
        }
    }

    // only jump if we have roughly a matching velocity towards our target rotation
    // jumps apply a multiplier to our current velocity, regardless of rotation
    // so if we are trying to change our yaw greatly this tick, we should not jump
    // because otherwise we will be moving still in our current velocity direction
    public static void safeJump(MovementState state, PlayerContext ctx) {
        Rotation targetRotation = state.getTarget().rotation();
        MutableVec3d playerVelocity = ctx.player().getVelocity();
        double velThreshold = 0.02;
        if (!BOT.isHorizontalCollision() && (Math.abs(playerVelocity.getX()) > velThreshold || Math.abs(playerVelocity.getZ()) > velThreshold)) {
            double velX = MathHelper.round(playerVelocity.getX(), 3);
            double velZ = MathHelper.round(playerVelocity.getZ(), 3);
            // we really only care about the x and z components of the velocity
            // because we are only concerned with the yaw
            Vector3d rotationVec = MathHelper.calculateViewVector(targetRotation.yaw(), targetRotation.pitch());
            double rotationX = MathHelper.round(rotationVec.getX(), 2);
            double rotationZ = MathHelper.round(rotationVec.getZ(), 2);
            if (Math.abs(rotationX) > 0.1 && Math.abs(velX) > velThreshold) {
                if (MathHelper.sign(rotationX) != MathHelper.sign(velX)) {
                    return;
                }
            }
            if (Math.abs(rotationZ) > 0.1 && Math.abs(velZ) > velThreshold) {
                if (MathHelper.sign(rotationZ) != MathHelper.sign(velZ)) {
                    return;
                }
            }
        }
        state.setInput(PathInput.JUMP, true);
    }

    // assumes we are always rotated towards dest center from src

    public static void centerSideways(MovementState state, PlayerContext ctx, BlockPos src, BlockPos dest) {
        Vector3i toDest = src.directionTo(dest);
        if (toDest.equals(Vector3i.ZERO)) {
            return;
        }
        // check if toDest is diagonal
        boolean diagonal = toDest.getX() != 0 && toDest.getZ() != 0;

        if (diagonal) {
            // todo: find better solution to pathologic error case:
            //  we are moving diagonal and our side block on one side is obstructed
            //  we hit the edge of the src block towards dest
            //  if we move left, our movement cancels out to 0 as we are being pushed back into the src block
            if (ctx.player().isHorizontalCollision()) return;
            // ensure we are centered on the src block in the direction of the dest block
            // so we avoid hitting blocks on the side mid-jump
            // e.g. if are jumping to -x -z:
            //  we want to be in roughly a parallelogram on the src block
            //  meaning we want our x and z to be roughly the same distance from the center
            double xDiff = Math.abs((src.x() + 0.5) - ctx.player().getX());
            double zDiff = Math.abs((src.z() + 0.5) - ctx.player().getZ());
            if (Math.abs(xDiff - zDiff) > 0.15) {
                boolean moveLeft = (xDiff > zDiff) == (toDest.getX() == toDest.getZ());
//                PATH_LOG.info("Diagonal centering: moving {} [{}, {}, {}]", moveLeft ? "left" : "right", ctx.player().getX(), ctx.player().getY(), ctx.player().getZ());
                state.setInput(moveLeft ? PathInput.MOVE_LEFT : PathInput.MOVE_RIGHT, true);
            }
        } else {
            // ensure we are centered on the src block in the perpendicular direction
            // so we avoid hitting blocks on the side mid-jump
            // e.g. if are jumping to -x:
            //  we can be any x, but our z must be within [src.z + 0.3, src.z + 0.7]
            if (toDest.getX() != 0) {
                double zDiff = (src.z() + 0.5) - ctx.player().getZ();
                if (Math.abs(zDiff) > 0.3) {
                    state.setInput(zDiff > 0 ? PathInput.MOVE_LEFT : PathInput.MOVE_RIGHT, true);
                }
            } else if (toDest.getZ() != 0) {
                double xDiff = (src.x() + 0.5) - ctx.player().getX();
                if (Math.abs(xDiff) > 0.3) {
                    state.setInput(xDiff > 0 ? PathInput.MOVE_LEFT : PathInput.MOVE_RIGHT, true);
                }
            }
        }
    }

    /**
     * Returns whether or not the specified block is
     * water, regardless of whether or not it is flowing.
     */
    public static boolean isWater(Block block) {
        return World.isWater(block);
    }

    /**
     * Returns whether or not the block at the specified pos is
     * water, regardless of whether or not it is flowing.
     *
     * @param bp  The block pos
     * @return Whether or not the block is water
     */
    public static boolean isWater(BlockPos bp) {
        return isWater(BlockStateInterface.getBlock(bp));
    }

    public static boolean isLava(Block block) {
        return block == BlockRegistry.LAVA;
    }

    /**
     * Returns whether or not the specified pos has a liquid
     *
     * @param p   The pos
     * @return Whether or not the block is a liquid
     */
    public static boolean isLiquid(BlockPos p) {
        return isLiquid(BlockStateInterface.getBlock(p));
    }

    public static boolean isLiquid(Block block) {
        return World.isFluid(block);
    }

    public static boolean isPlayerTouchingWater() {
        return PlayerContext.INSTANCE.player().isTouchingWater() || MovementHelper.isWater(World.getBlock(PlayerContext.INSTANCE.playerFeet()));
    }

    public static boolean isPlayerTouchingLava() {
        return PlayerContext.INSTANCE.player().isTouchingLava() || MovementHelper.isLava(World.getBlock(PlayerContext.INSTANCE.playerFeet()));
    }

    public static boolean isPlayerTouchingLiquid() {
        return isPlayerTouchingWater() || isPlayerTouchingLava();
    }

//    static boolean possiblyFlowing(BlockState state) {
//        if (World.isFluid(state.block())) {
//            return World.getFluidFlow(state) != MutableVec3d.ZERO;
//        }
//        return false;
//    }

    public static boolean isFlowing(int x, int y, int z) {
        return !World.getFluidFlow(x, y, z).equals(MutableVec3d.ZERO);
//        FluidState fluidState = state.getFluidState();
//        if (!(fluidState.getType() instanceof FlowingFluid)) {
//            return false;
//        }
//        if (fluidState.getType().getAmount(fluidState) != 8) {
//            return true;
//        }
//        return possiblyFlowing(bsi.get0(x + 1, y, z))
//            || possiblyFlowing(bsi.get0(x - 1, y, z))
//            || possiblyFlowing(bsi.get0(x, y, z + 1))
//            || possiblyFlowing(bsi.get0(x, y, z - 1));
    }

    public static boolean isBlockNormalCube(int blockStateId) {
        Block block = BLOCK_DATA.getBlockDataFromBlockStateId(blockStateId);
        if (!BLOCK_DATA.isShapeFullBlock(blockStateId)) return false;
        if (block == BlockRegistry.BAMBOO
            || block == BlockRegistry.MOVING_PISTON
            || block == BlockRegistry.SCAFFOLDING
            || block.blockTags().contains(BlockTags.SHULKER_BOXES)
            || block == BlockRegistry.POINTED_DRIPSTONE
            || (block.name().contains("amethyst") && block != BlockRegistry.AMETHYST_BLOCK)
        ) {
            return false;
        }
        return true;
    }

    public static PlaceResult attemptToPlaceABlock(MovementState state, BlockPos placeAt, boolean preferDown, boolean wouldSneak) {
        PlayerContext ctx = BARITONE.getPlayerContext();
        Optional<Rotation> direct = RotationUtils.reachable(ctx, placeAt, wouldSneak); // we assume that if there is a block there, it must be replacable
        boolean found = false;
        if (direct.isPresent()) {
            state.setTarget(new MovementTarget(direct.get(), true));
            found = true;
        }
        for (int i = 0; i < 5; i++) {
            BlockPos against1 = placeAt.relative(HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[i]);
            if (MovementHelper.canPlaceAgainst(against1)) {
                if (!BARITONE.getInventoryBehavior().selectThrowawayForLocation(false, placeAt.x(), placeAt.y(), placeAt.z())) { // get ready to place a throwaway block
                    PATH_LOG.info("No throwaway blocks found in inventory :(");
                    state.setStatus(MovementStatus.UNREACHABLE);
                    return PlaceResult.NO_OPTION;
                }
                double faceX = (placeAt.x() + against1.x() + 1.0D) * 0.5D;
                double faceY = (placeAt.y() + against1.y() + 0.5D) * 0.5D;
                double faceZ = (placeAt.z() + against1.z() + 1.0D) * 0.5D;
                Vector3d playerEyes = wouldSneak
                    ? ctx.playerHead().add(0, -(1.62 - 1.27), 0)
                    : ctx.playerHead();
                Rotation place = RotationUtils.calcRotationFromVec3d(
                    playerEyes,
                    Vector3d.from(faceX, faceY, faceZ),
                    ctx.playerRotations());
                BlockRaycastResult blockRaycastResult = RaycastHelper.blockRaycastFromPos(
                    playerEyes.getX(), playerEyes.getY(), playerEyes.getZ(),
                    place.yaw(), place.pitch(),
                    ctx.player().getBlockReachDistance(),
                    false
                );
                if (blockRaycastResult.hit()) {
                    boolean againstCorrect = blockRaycastResult.x() == against1.x() && blockRaycastResult.y() == against1.y() && blockRaycastResult.z() == against1.z();
                    var rel = blockRaycastResult.direction().getNormal().add(blockRaycastResult.x(), blockRaycastResult.y(), blockRaycastResult.z());
                    boolean faceCorrect = rel.getX() == placeAt.x() && rel.getY() == placeAt.y() && rel.getZ() == placeAt.z();
                    if (againstCorrect && faceCorrect) {
                        state.setTarget(new MovementTarget(place, true));
                        found = true;
                        if (!preferDown) {
                            // if preferDown is true, we want the last option
                            // if preferDown is false, we want the first
                            break;
                        }
                    }
                }
            }
        }
        if (ctx.getSelectedBlock().isPresent()) {
            BlockPos selectedBlock = ctx.getSelectedBlock().get();
            BlockRaycastResult raycast = ctx.objectMouseOver();
            Direction side = raycast.direction();
            // only way for selectedBlock.equals(placeAt) to be true is if it's replacable
            if (selectedBlock.equals(placeAt) || (MovementHelper.canPlaceAgainst(selectedBlock) && selectedBlock.relative(side).equals(placeAt))) {
                if (wouldSneak) {
                    state.setInput(PathInput.SNEAK, true);
                }
                BARITONE.getInventoryBehavior().selectThrowawayForLocation(true, placeAt.x(), placeAt.y(), placeAt.z());
                return PlaceResult.READY_TO_PLACE;
            }
        }
        if (found) {
            if (wouldSneak) {
                state.setInput(PathInput.SNEAK, true);
            }
            BARITONE.getInventoryBehavior().selectThrowawayForLocation(true, placeAt.x(), placeAt.y(), placeAt.z());
            return PlaceResult.ATTEMPTING;
        }
        return PlaceResult.NO_OPTION;
    }

    public enum PlaceResult {
        READY_TO_PLACE, ATTEMPTING, NO_OPTION;
    }

//    static boolean isTransparent(Block b) {
//        return b BLOCK_DATA.isAir ||
//            b == BlockRegistry.LAVA ||
//            b == BlockRegistry.WATER;
//    }
}
