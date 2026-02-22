package com.zenith.feature.pathfinder.movement;

import com.zenith.feature.pathfinder.BlockStateInterface;
import com.zenith.feature.pathfinder.PrecomputedData;
import com.zenith.feature.pathfinder.goals.Goal;
import com.zenith.feature.pathfinder.util.ToolSet;
import com.zenith.feature.player.World;
import com.zenith.mc.block.Block;
import com.zenith.mc.block.BlockPos;
import com.zenith.mc.block.BlockRegistry;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static com.zenith.Globals.*;
import static com.zenith.feature.pathfinder.movement.ActionCosts.COST_INF;

public class CalculationContext {

    public final ToolSet toolSet = new ToolSet();
    public final boolean hasThrowaway = CONFIG.client.extra.pathfinder.allowPlace && BARITONE.getInventoryBehavior().hasGenericThrowaway();
//    public final boolean hasWaterBucket = Baritone.settings().allowWaterBucketFall.value && Inventory.isHotbarSlot(player.getInventory().findSlotMatchingItem(STACK_BUCKET_WATER)) && world.dimension() != Level.NETHER;
    public final boolean canSprint = CONFIG.client.extra.pathfinder.allowSprint && CACHE.getPlayerCache().getThePlayer().getFood() > 6;
    protected final double placeBlockCost = CONFIG.client.extra.pathfinder.blockPlacementPenalty; // protected because you should call the function instead
    public final boolean allowBreak = CONFIG.client.extra.pathfinder.allowBreak;
    public final List<Block> allowBreakAnyway = CONFIG.client.extra.pathfinder.allowBreakAnyway.stream()
        .map(BlockRegistry.REGISTRY::get)
        .filter(Objects::nonNull)
        .toList();
    public final boolean allowParkour = CONFIG.client.extra.pathfinder.allowParkour;
    public final boolean allowParkourPlace = CONFIG.client.extra.pathfinder.allowParkourPlace;
//    public final boolean allowJumpAt256;
    public final boolean allowParkourAscend = CONFIG.client.extra.pathfinder.allowParkourAscend;
    public final boolean assumeWalkOnWater = false; // Baritone.settings().assumeWalkOnWater.value;
//    public boolean allowFallIntoLava;
    public final int frostWalker = 0;
    public final boolean allowDiagonalDescend = CONFIG.client.extra.pathfinder.allowDiagonalDescend;
    public final boolean allowDiagonalAscend = CONFIG.client.extra.pathfinder.allowDiagonalAscend;
    public final boolean allowDownward = CONFIG.client.extra.pathfinder.allowDownward;
    public final int minFallHeight = 3;
    public final int maxFallHeightNoWater = CONFIG.client.extra.pathfinder.maxFallHeightNoWater;
    public final boolean allowLongFall = CONFIG.client.extra.pathfinder.allowLongFall;
    public final double longFallCostLogMultiplier = CONFIG.client.extra.pathfinder.longFallCostLogMultiplier;
    public final double longFallCostAddCost = CONFIG.client.extra.pathfinder.longFallCostAddCost;
//    public final int maxFallHeightBucket;
    public final double waterWalkSpeed = ActionCosts.WALK_ONE_IN_WATER_COST + ActionCosts.WALK_ONE_BLOCK_COST;
    public final double lavaWalkSpeed = CONFIG.client.extra.pathfinder.lavaWalkCost + ActionCosts.WALK_ONE_BLOCK_COST;
    public final double breakBlockAdditionalCost = CONFIG.client.extra.pathfinder.blockBreakAdditionalCost;
    public final double backtrackCostFavoringCoefficient = 0.5;
    public final double jumpPenalty = CONFIG.client.extra.pathfinder.jumpPenalty;
    public final double walkOnWaterOnePenalty = 3;

    public final PrecomputedData precomputedData = PrecomputedData.INSTANCE;
    @Nullable public final Goal goal;

    public CalculationContext(Goal goal) {
        this.goal = goal;
    }
    public CalculationContext() {
        this(null);
    }

    public double costOfPlacingAt(CalculationContext context, int x, int y, int z, int current) {
        if (!hasThrowaway) { // only true if allowPlace is true, see constructor
            return COST_INF;
        }
        if (context.goal != null && context.goal.isInGoal(x, y, z)) {
            return 0;
        }
        return placeBlockCost;
    }

    public double breakCostMultiplierAt(int x, int y, int z, int current) {
        if (!allowBreak && !allowBreakAnyway.contains(BlockStateInterface.getBlock(current))) {
            return COST_INF;
        }
        return 1;
    }

    public boolean isLoaded(final int x, final int z) {
        return BlockStateInterface.isLoaded(x, z);
    }

    public int getId(final BlockPos pos) {
        return getId(pos.x(), pos.y(), pos.z());
    }

    public Block getBlock(int x, int y, int z) {
        return World.getBlock(x, y, z);
    }

    public int getId(int x, int y, int z) {
        return World.getBlockStateId(x, y, z);
    }
}
