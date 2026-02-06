package com.zenith.feature.pathfinder.process;

import com.zenith.cache.data.inventory.Container;
import com.zenith.feature.pathfinder.*;
import com.zenith.feature.pathfinder.goals.*;
import com.zenith.feature.pathfinder.movement.CalculationContext;
import com.zenith.feature.pathfinder.movement.MovementHelper;
import com.zenith.feature.pathfinder.util.BlockOptionalMetaLookup;
import com.zenith.feature.pathfinder.util.RotationUtils;
import com.zenith.feature.pathfinder.util.WorldScanner;
import com.zenith.feature.player.Rotation;
import com.zenith.feature.player.World;
import com.zenith.mc.block.Block;
import com.zenith.mc.block.BlockPos;
import com.zenith.mc.block.BlockRegistry;
import com.zenith.mc.item.ItemRegistry;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

import static com.zenith.Globals.*;
import static com.zenith.feature.pathfinder.movement.ActionCosts.COST_INF;

public final class MineProcess extends BaritoneProcessHelper implements IBaritoneProcess {

    private PathingRequestFuture future;
    private BlockOptionalMetaLookup filter;
    private List<BlockPos> knownOreLocations;
    private List<BlockPos> blacklist; // inaccessible
    private Map<BlockPos, Long> anticipatedDrops;
    private BlockPos branchPoint;
    private GoalRunAway branchPointRunaway;
    private int desiredQuantity;
    private int tickCount;
    private boolean initialScanCompleted;

    public MineProcess(Baritone baritone) {
        super(baritone);
    }

    @Override
    public boolean isActive() {
        return filter != null;
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel, final Goal currentGoal, final PathingCommand prevCommand) {
        if (desiredQuantity > 0) {
            int curr = CACHE.getPlayerCache().getInventoryCache().getPlayerInventory().getContents().stream()
                .filter(stack -> stack != Container.EMPTY_STACK)
                .mapToInt(stack -> {
                    var itemId = stack.getId();
                    var itemData = ItemRegistry.REGISTRY.get(itemId);
                    if (itemData == null) return 0;
                    var blockWithMatchingName = BlockRegistry.REGISTRY.get(itemData.name());
                    if (blockWithMatchingName != null) {
                        return stack.getAmount();
                    }
                    return 0;
                })
                .sum();
            PATH_LOG.info("Currently have " + curr + " valid items");
            if (curr >= desiredQuantity) {
                PATH_LOG.info("Have " + curr + " valid items");
                future.complete(true);
                future.notifyListeners();
                cancel();
                return null;
            }
        }
        if (calcFailed) {
            if (!knownOreLocations.isEmpty()) {
                PATH_LOG.info("Unable to find any path to {}, blacklisting presumably unreachable closest instance...", filter);
                knownOreLocations.stream().min(Comparator.comparingDouble(ctx.playerFeet()::squaredDistance)).ifPresent(blacklist::add);
                knownOreLocations.removeIf(blacklist::contains);
            } else {
                PATH_LOG.info("Unable to find any path to {}, canceling mine", filter);
                cancel();
                return null;
            }
        }

        updateLoucaSystem();
        int mineGoalUpdateInterval = 50; // Baritone.settings().mineGoalUpdateInterval.value;
        List<BlockPos> curr = new ArrayList<>(knownOreLocations);
        if (tickCount++ % mineGoalUpdateInterval == 0) { // big brain
            CalculationContext context = new CalculationContext(currentGoal);
            Baritone.getExecutor().execute(() -> rescan(curr, context));
        }
        Optional<BlockPos> shaft = curr.stream()
                .filter(pos -> pos.x() == ctx.playerFeet().x() && pos.z() == ctx.playerFeet().z())
                .filter(pos -> pos.y() >= ctx.playerFeet().y())
//                .filter(pos -> !(BLOCK_DATA.isAir(BlockStateInterface.get(pos).block()))) // after breaking a block, it takes mineGoalUpdateInterval ticks for it to actually update this list =(
                .min(Comparator.comparingDouble(ctx.playerFeet().above()::squaredDistance));
        baritone.getInputOverrideHandler().clearAllKeys();
        if (shaft.isPresent() && ctx.player().isOnGround()) {
            BlockPos pos = shaft.get();
            int state = BlockStateInterface.getId(pos);
            if (!MovementHelper.avoidBreaking(pos.x(), pos.y(), pos.z(), state)) {
                Optional<Rotation> rot = RotationUtils.reachable(ctx, pos);
                if (rot.isPresent() && isSafeToCancel) {
                    baritone.getLookBehavior().updateRotation(rot.get());
                    MovementHelper.switchToBestToolFor(World.getBlock(pos));
                    if (ctx.isLookingAt(pos) || ctx.playerRotations().isReallyCloseTo(rot.get())) {
                        baritone.getInputOverrideHandler().setClickTarget(pos);
                        baritone.getInputOverrideHandler().setInputForceState(PathInput.LEFT_CLICK_BLOCK, true);
                    }
                    return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
                }
            }
        }
        PathingCommand command = updateGoal();
        if (command == null) {
            // none in range
            // maybe say something in chat? (ahem impact)
            cancel();
            return null;
        }
        return command;
    }


    private void updateLoucaSystem() {
        Map<BlockPos, Long> copy = new HashMap<>(anticipatedDrops);
        ctx.getSelectedBlock().ifPresent(pos -> {
            if (knownOreLocations.contains(pos)) {
                copy.put(pos, System.currentTimeMillis() + 250); // Baritone.settings().mineDropLoiterDurationMSThanksLouca.value);
            }
        });
        // elaborate dance to avoid concurrentmodificationexcepption since rescan thread reads this
        // don't want to slow everything down with a gross lock do we now
        for (BlockPos pos : anticipatedDrops.keySet()) {
            if (copy.get(pos) < System.currentTimeMillis()) {
                copy.remove(pos);
            }
        }
        anticipatedDrops = copy;
    }

    @Override
    public void onLostControl() {
        mine(0, (BlockOptionalMetaLookup) null);
    }

    @Override
    public String displayName0() {
        return "Mine " + filter;
    }

    private PathingCommand updateGoal() {
        BlockOptionalMetaLookup filter = filterFilter();
        if (filter == null) {
            return null;
        }

        List<BlockPos> locs = knownOreLocations;
        if (!locs.isEmpty()) {
            CalculationContext context = new CalculationContext();
            List<BlockPos> locs2 = prune(context, new ArrayList<>(locs), filter, 64, blacklist, droppedItemsScan());
            // can't reassign locs, gotta make a new var locs2, because we use it in a lambda right here, and variables you use in a lambda must be effectively final
            Goal goal = new GoalComposite(locs2.stream().map(loc -> coalesce(loc, locs2, context)).toArray(Goal[]::new));
            knownOreLocations = locs2;
//            PATH_LOG.info("Mine goal updated to " + goal);
            return new PathingCommand(goal, PathingCommandType.REVALIDATE_GOAL_AND_PATH);
        }
        // we don't know any ore locations at the moment
        // only when we should explore for blocks or are in legit mode we do this
        if (!initialScanCompleted) return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
        int y = -59;
        if (branchPoint == null) {
            branchPoint = ctx.playerFeet();
        }
        // TODO shaft mode, mine 1x1 shafts to either side
        // TODO also, see if the GoalRunAway with maintain Y at 11 works even from the surface
        if (branchPointRunaway == null) {
            branchPointRunaway = new GoalRunAway(1, y, branchPoint) {
                @Override
                public boolean isInGoal(int x, int y, int z) {
                    return false;
                }

                @Override
                public double heuristic() {
                    return Double.NEGATIVE_INFINITY;
                }
            };
        }
//        PATH_LOG.info("No known ore locations, exploring for blocks");
        return new PathingCommand(branchPointRunaway, PathingCommandType.REVALIDATE_GOAL_AND_PATH);
    }

    private synchronized void rescan(List<BlockPos> already, CalculationContext context) {
        BlockOptionalMetaLookup filter = filterFilter();
        if (filter == null) {
            return;
        }
        List<BlockPos> dropped = droppedItemsScan();
        List<BlockPos> locs = searchWorld(context, filter, 64, already, blacklist, dropped);
        locs.addAll(dropped);
        knownOreLocations = locs;
        this.initialScanCompleted = true;
    }

    private boolean internalMiningGoal(BlockPos pos, CalculationContext context, List<BlockPos> locs) {
        // Here, BlockStateInterface is used because the position may be in a cached chunk (the targeted block is one that is kept track of)
        if (locs.contains(pos)) {
            return true;
        }
        Block state = BlockStateInterface.getBlock(pos);
        if (state.isAir()) {
            return true;
        }
        return filter.getBlockSet().contains(state) && plausibleToBreak(context, pos);
    }

    private Goal coalesce(BlockPos loc, List<BlockPos> locs, CalculationContext context) {
        boolean assumeVerticalShaftMine = !BlockStateInterface.getBlock(loc.above()).fallingBlock();
        boolean upwardGoal = internalMiningGoal(loc.above(), context, locs);
        boolean downwardGoal = internalMiningGoal(loc.below(), context, locs);
        boolean doubleDownwardGoal = internalMiningGoal(loc.below(2), context, locs);
        if (upwardGoal == downwardGoal) { // symmetric
            if (doubleDownwardGoal && assumeVerticalShaftMine) {
                // we have a checkerboard like pattern
                // this one, and the one two below it
                // therefore it's fine to path to immediately below this one, since your feet will be in the doubleDownwardGoal
                // but only if assumeVerticalShaftMine
                return new GoalThreeBlocks(loc);
            } else {
                // this block has nothing interesting two below, but is symmetric vertically so we can get either feet or head into it
                return new GoalTwoBlocks(loc);
            }
        }
        if (upwardGoal) {
            // downwardGoal known to be false
            // ignore the gap then potential doubleDownward, because we want to path feet into this one and head into upwardGoal
            return new GoalBlock(loc);
        }
        // upwardGoal known to be false, downwardGoal known to be true
        if (doubleDownwardGoal && assumeVerticalShaftMine) {
            // this block and two below it are goals
            // path into the center of the one below, because that includes directly below this one
            return new GoalTwoBlocks(loc.below());
        }
        // upwardGoal false, downwardGoal true, doubleDownwardGoal false
        // just this block and the one immediately below, no others
        return new GoalBlock(loc.below());
    }

    /**
     * Begin to search for and mine the specified blocks.
     *
     * @param filter The blocks to mine
     */
    public PathingRequestFuture mine(BlockOptionalMetaLookup filter) {
        return mine(0, filter);
    }

    /**
     * Begin to search for and mine the specified blocks.
     *
     * @param blocks The blocks to mine
     */
    public PathingRequestFuture mineByName(String... blocks) {
        return mineByName(0, blocks);
    }

    /**
     * Begin to search for and mine the specified blocks.
     *
     * @param quantity The total number of items to get
     * @param blocks   The blocks to mine
     */
    public PathingRequestFuture mine(int quantity, Block... blocks) {
        return mine(quantity, new BlockOptionalMetaLookup(blocks));
    }

    /**
     * Begin to search for and mine the specified blocks.
     *
     * @param blocks The blocks to mine
     */
    public PathingRequestFuture mine(Block... blocks) {
        return mine(0, blocks);
    }

    /**
     * Cancels the current mining task
     */
    public void cancel() {
        onLostControl();
    }

    private record GoalThreeBlocks(int x, int y, int z) implements Goal {

        public GoalThreeBlocks(BlockPos pos) {
            this(pos.x(), pos.y(), pos.z());
        }

        @Override
        public boolean isInGoal(int x, int y, int z) {
            return x == this.x && (y == this.y || y == this.y - 1 || y == this.y - 2) && z == this.z;
        }

        @Override
        public double heuristic(int x, int y, int z) {
            int xDiff = x - this.x;
            int yDiff = y - this.y;
            int zDiff = z - this.z;
            return GoalBlock.calculate(xDiff, yDiff < -1 ? yDiff + 2 : yDiff == -1 ? 0 : yDiff, zDiff);
        }

        @Override
        public int hashCode() {
            return (int) BlockPos.longHash(x, y, z) * 516508351 * 393857768;
        }

        @Override
        public String toString() {
            return String.format(
                    "GoalThreeBlocks{x=%s,y=%s,z=%s}",
                    x,
                    y,
                    z
            );
        }
    }

    public List<BlockPos> droppedItemsScan() {
        List<BlockPos> ret = new ArrayList<>();
        var items = CACHE.getEntityCache().getEntities().values().stream()
            .filter(e -> e.getEntityType() == EntityType.ITEM)
            .toList();
        // todo: this will not work for all blocks
        //  like stone drops cobblestone
        //  we would need an actual loot table to check against
        //  there could be a simpler way to do this, like setting a movement goal to the positions we are breaking
        for (var itemEntity : items) {
            var itemStack = itemEntity.getMetadataValue(8, MetadataTypes.ITEM, ItemStack.class);
            if (itemStack == null) continue;
            var itemData = ItemRegistry.REGISTRY.get(itemStack.getId());
            if (itemData == null) continue;
            // will only work for certain blocks (or silk touch) like ancient debris
            var blockWithMatchingName = BlockRegistry.REGISTRY.get(itemData.name());
            if (blockWithMatchingName != null) {
                if (filter.has(blockWithMatchingName)) {
                    ret.add(itemEntity.blockPos());
                    break;
                }
            }
        }
        ret.addAll(anticipatedDrops.keySet());
        return ret;
    }

    public static List<BlockPos> searchWorld(CalculationContext ctx, BlockOptionalMetaLookup filter, int max, List<BlockPos> alreadyKnown, List<BlockPos> blacklist, List<BlockPos> dropped) {
        List<BlockPos> locs = new ArrayList<>();
        List<Block> untracked = new ArrayList<>();
        untracked.addAll(filter.getBlockSet());

        locs = prune(ctx, locs, filter, max, blacklist, dropped);

        if (!untracked.isEmpty() || locs.size() < max) {
            locs.addAll(WorldScanner.scanChunkRadius(filter, max, 10, 32));
        }

        locs.addAll(alreadyKnown);

        return prune(ctx, locs, filter, max, blacklist, dropped);
    }

    private static List<BlockPos> prune(CalculationContext ctx, List<BlockPos> locs2, BlockOptionalMetaLookup filter, int max, List<BlockPos> blacklist, List<BlockPos> dropped) {
        dropped.removeIf(drop -> {
            for (BlockPos pos : locs2) {
                if (pos.squaredDistance(drop) <= 9 && filter.getBlockSet().contains(ctx.getBlock(pos.x(), pos.y(), pos.z())) && MineProcess.plausibleToBreak(ctx, pos)) { // TODO maybe drop also has to be supported? no lava below?
                    return true;
                }
            }
            return false;
        });
        List<BlockPos> locs = locs2
            .stream()
            .distinct()

            // remove any that are within loaded chunks that aren't actually what we want
            .filter(pos -> !BlockStateInterface.worldContainsLoadedChunk(pos.x(), pos.z()) || filter.getBlockSet().contains(ctx.getBlock(pos.x(), pos.y(), pos.z())) || dropped.contains(pos))

            // remove any that are implausible to mine (encased in bedrock, or touching lava)
            .filter(pos -> MineProcess.plausibleToBreak(ctx, pos))

            .filter(pos -> !blacklist.contains(pos))

            .sorted(Comparator.comparingDouble(BARITONE.getPlayerContext().player().blockPosition()::squaredDistance))
            .collect(Collectors.toList());

        if (locs.size() > max) {
            return locs.subList(0, max);
        }
        return locs;
    }

    public static boolean plausibleToBreak(CalculationContext ctx, BlockPos pos) {
        var state = BlockStateInterface.getId(pos);
        if (MovementHelper.getMiningDurationTicks(ctx, pos.x(), pos.y(), pos.z(), state, true) >= COST_INF) {
            return false;
        }
        if (MovementHelper.avoidBreaking(pos.x(), pos.y(), pos.z(), state)) {
            return false;
        }

        // bedrock above and below makes it implausible, otherwise we're good
        return !(BlockStateInterface.getBlock(pos.above()) == BlockRegistry.BEDROCK && BlockStateInterface.getBlock(pos.below()) == BlockRegistry.BEDROCK);
    }

    public PathingRequestFuture mineByName(int quantity, String... blocks) {
        Set<Block> blockList = new HashSet<>();
        for (String blockName : blocks) {
            Block b = BlockRegistry.REGISTRY.get(blockName);
            if (b != null) blockList.add(b);
        }
        return mine(quantity, new BlockOptionalMetaLookup(blockList));
    }

    public PathingRequestFuture mine(int quantity, BlockOptionalMetaLookup filter) {
        if (future != null && !future.isCompleted()) {
            future.complete(false);
        }
        this.future = new PathingRequestFuture();
        this.filter = filter;
        if (this.filterFilter() == null) {
            this.filter = null;
        }
        this.desiredQuantity = quantity;
        this.knownOreLocations = new ArrayList<>();
        this.blacklist = new ArrayList<>();
        this.branchPoint = null;
        this.branchPointRunaway = null;
        this.anticipatedDrops = new HashMap<>();
//        if (filter != null) {
//            rescan(new ArrayList<>(), new CalculationContext());
//        }
        this.initialScanCompleted = false;
        return this.future;
    }

    private BlockOptionalMetaLookup filterFilter() {
        if (this.filter == null) {
            return null;
        }
        if (!CONFIG.client.extra.pathfinder.allowBreak) {
            var allowBreakAnyway = this.filter.getBlockSet().stream()
                .filter(block -> CONFIG.client.extra.pathfinder.allowBreakAnyway.contains(block.name()))
                .collect(Collectors.toSet());
            if (allowBreakAnyway.isEmpty()) {
                // todo: discord noti?
                error("Unable to mine when allowBreak is off and target block is not in allowBreakAnyway!");
                return null;
            } else {
                return new BlockOptionalMetaLookup(allowBreakAnyway);
            }
        }
        return filter;
    }
}
