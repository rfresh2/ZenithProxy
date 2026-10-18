package com.zenith.feature.pathfinder.process;

import com.zenith.Proxy;
import com.zenith.cache.data.chunk.Chunk;
import com.zenith.feature.pathfinder.*;
import com.zenith.feature.pathfinder.goals.Goal;
import com.zenith.feature.pathfinder.goals.GoalBlock;
import com.zenith.feature.pathfinder.goals.GoalXZ;
import com.zenith.feature.pathfinder.process.elytra.ElytraBehavior;
import com.zenith.feature.pathfinder.process.elytra.NetherPathfinderContext;
import com.zenith.feature.pathfinder.util.RotationUtils;
import com.zenith.feature.player.*;
import com.zenith.mc.block.Block;
import com.zenith.mc.block.BlockPos;
import com.zenith.mc.block.BlockRegistry;
import com.zenith.mc.dimension.DimensionRegistry;
import com.zenith.mc.item.ItemRegistry;
import com.zenith.util.ItemUtil;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.zenith.Globals.CACHE;

public class ElytraProcess extends BaritoneProcessHelper {
    public State state;
    private boolean goingToLandingSpot;
    private BlockPos landingSpot;
    private boolean reachedGoal; // this basically just prevents potential notification spam
    private volatile ElytraBehavior behavior;
    private NetherPathfinderContext npfContext;
    private PathingRequestFuture future;
    private boolean emergencyLanding;

    private static final int SHORT_LANDING_COLUMN_HEIGHT = 15;
    private static final int LONG_LANDING_COLUMN_HEIGHT = 39;
    private static final long LANDING_SEARCH_BUDGET_NANOS = TimeUnit.MILLISECONDS.toNanos(25); // half a tick
    private int landingColumnHeight = SHORT_LANDING_COLUMN_HEIGHT;
    private final Set<BlockPos> badLandingSpots = new HashSet<>();
    private LandingSearchState landingSearchState;
    private long startedJumpStateTime = 0L;

    @Override
    public void onLostControl() {
        onLostControl(true);
    }

    public synchronized void onLostControl(boolean destroyNpf) {
        this.state = State.START_FLYING; // TODO: null state?
        this.goingToLandingSpot = false;
        this.landingSpot = null;
        this.landingSearchState = null;
        this.reachedGoal = false;
        startedJumpStateTime = 0L;
        destroyBehaviorAsync();
        if (destroyNpf) {
            destroyNpfContextAsync();
            if (this.future != null && !this.future.isCompleted()) {
                this.future.complete(false);
            }
            this.future = null;
            this.emergencyLanding = false;
        }
    }

    public ElytraProcess(Baritone baritone) {
        super(baritone);
    }


    @Override
    public boolean isActive() {
        return this.behavior != null;
    }

    public void resetState() {
        var destination = this.currentDestination();
        this.onLostControl();
        if (destination != null) {
            this.pathTo(destination);
        }
    }

    @Override
    public synchronized PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel, Goal currentGoal, PathingCommand prevCommand) {
        if (this.behavior == null) return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
        this.behavior.onTick();

        if (calcFailed || this.behavior.initialPathFailed) {
            onLostControl();
            return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
        }

        boolean safetyLanding = false;
        if (ctx.player().isFallFlying() && shouldLandForSafety()) {
            if (true /* Baritone.settings().elytraAllowEmergencyLand */) {
                info("Emergency landing - almost out of elytra durability or fireworks");
                safetyLanding = true;
            } else {
                info("almost out of elytra durability or fireworks, but I'm going to continue since elytraAllowEmergencyLand is false");
            }
        }
        if (ctx.player().isFallFlying() && this.state != State.LANDING && (this.behavior.pathManager.isComplete() || safetyLanding)) {
            var last = this.behavior.pathManager.path.getLast();
            if (last != null && (ctx.playerFeetAsVec().distanceSquared(Vector3d.from(last.x() + 0.5, last.y() + 0.5, last.z() + 0.5)) < (48 * 48) || safetyLanding) && (!goingToLandingSpot || (safetyLanding && this.landingSpot == null))) {
                if (this.landingSearchState == null) {
                    info("Path complete, searching for safe landing spot...");
                }
                var landingSpot = findSafeLandingSpot(ctx.playerFeet());
                // if this fails we will just keep orbiting the last node until we run out of rockets or the user intervenes
                if (landingSpot != null) {
                    info("Found potential landing spot.");
                    this.emergencyLanding |= safetyLanding;
                    this.pathTo0(landingSpot, true);
                    this.landingSpot = landingSpot;
                    this.goingToLandingSpot = true;
                } else {
                    this.goingToLandingSpot = false;
                }
            }

            if (last != null && ctx.playerFeetAsVec().distanceSquared(Vector3d.from(last.x() + 0.5, last.y() + 0.5, last.z() + 0.5)) < 1) {
                if (true /* Baritone.settings().notificationOnPathComplete */ && !reachedGoal) {
                    info("Pathing complete");
                }
                reachedGoal = true;

                // we are goingToLandingSpot and we are in the last node of the path
                if (this.goingToLandingSpot && landingSpot != null) {
                    this.state = State.LANDING;
                    info("Above the landing spot, landing...");
                }
            }
        }

        if (this.state == State.LANDING) {
            var endPos = this.landingSpot != null ? this.landingSpot : behavior.pathManager.path.getLast();
            if (ctx.player().isFallFlying() && endPos != null) {
                var from = ctx.playerFeetAsVec();
                var to = Vector3d.from(((double) endPos.x()) + 0.5, from.getY(), ((double) endPos.z()) + 0.5);
                var rotation = RotationUtils.calcRotationFromVec3d(from, to, ctx.playerRotations());
                baritone.getLookBehavior().updateRotation(new Rotation(rotation.yaw(), 0)); // this will be overwritten, probably, by behavior tick

                if (ctx.playerFeetAsVec().getY() < endPos.y() - this.landingColumnHeight) {
                    info("bad landing spot, trying again...");
                    landingSpotIsBad(endPos);
                }
            }
        }

        if (ctx.player().isFallFlying()) {
            if (state != State.LANDING) state = State.FLYING;
            behavior.landingMode = this.state == State.LANDING;
            baritone.getInputOverrideHandler().clearAllKeys();
            if (this.behavior.npfContext.tryAcquireReadLock()) {
                try {
                    behavior.tick();
                } finally {
                    this.behavior.npfContext.releaseReadLock();
                }
            }
            return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
        } else if (this.state == State.LANDING) {
            if (ctx.playerMotion().mul(1, 0, 1).length() > 0.001) {
                info("Landed, but still moving, waiting for velocity to die down... ");
                baritone.getInputOverrideHandler().setInputForceState(PathInput.SNEAK, true);
                return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
            }
            info("Done :)");
            baritone.getInputOverrideHandler().clearAllKeys();
            var completedFuture = this.future;
            this.future = null;
            boolean success = !this.emergencyLanding;
            this.onLostControl();
            if (completedFuture != null) {
                completedFuture.complete(success);
                if (success) completedFuture.notifyListeners();
            }
            return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
        }

        if (this.state == State.FLYING || this.state == State.START_FLYING) {
            this.state = ctx.player().isOnGround() && true /* Baritone.settings().elytraAutoJump */
                    ? State.JUMP
                    : State.START_FLYING;
        }

        if (this.state == State.JUMP) {
            if (startedJumpStateTime == 0L) {
                startedJumpStateTime = System.currentTimeMillis();
            }
            if (shouldLandForSafety()) {
                info("Not taking off, because elytra durability or fireworks are so low that I would immediately emergency land anyway.");
                onLostControl();
                return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
            }
            final boolean isJumpFree = World.getCollidingBlockStatesInside(ctx.player().getPlayerCollisionBox().move(0, 0.5, 0)).isEmpty();
            if (ctx.player().isOnGround() && isJumpFree) {
                baritone.getInputOverrideHandler().setInputForceState(PathInput.JUMP, true);
                return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
            }
            final boolean canStartFlying = !ctx.player().isOnGround() && !ctx.player().isFallFlying();

            if (canStartFlying) {
                this.state = State.START_FLYING;
            } else {
                // todo: would be better if we searched for a safe pos to takeoff from, and then pathed directly there
                if (System.currentTimeMillis() - startedJumpStateTime > 2500) {
                    info("Unable to perform autoJump takeoff, pathing forward");
                    var goal = GoalXZ.fromDirection(
                        ctx.playerFeetAsVec(),
                        ctx.player().getYaw(),
                        5
                    );
                    return new PathingCommand(goal, PathingCommandType.REVALIDATE_GOAL_AND_PATH);
                }
                return new PathingCommand(null, PathingCommandType.SET_GOAL_AND_PATH);
            }
        }

        // yucky
        if (this.state == State.PAUSE) {
            return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
        }

        if (this.state == State.START_FLYING) {
            if (!isSafeToCancel) {
                // owned
                baritone.getPathingBehavior().secretInternalSegmentCancel();
            }
            boolean wasJumpPressed = baritone.getInputOverrideHandler().isInputForcedDown(PathInput.JUMP);
            baritone.getInputOverrideHandler().clearAllKeys();
            if (!ctx.player().isOnGround() && !wasJumpPressed) {
                baritone.getInputOverrideHandler().setInputForceState(PathInput.JUMP, true);
            }
        }
        return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
    }

    public synchronized void applyFlightInput() {
        if (behavior == null || !behavior.useFirework) return;
        var handler = baritone.getInputOverrideHandler();
        var input = handler.currentInput == null ? new Input() : new Input(handler.currentInput);
        input.rightClick = true;
        input.hand = behavior.fireworkHand;
        input.clickTarget = ClickTarget.None.INSTANCE;
        handler.currentInput = input;
    }

    public synchronized void onInputSubmitted(InputRequestFuture future) {
        var current = behavior;
        if (current == null || !current.useFirework) return;
        future.addInputExecutedListener(result -> {
            if (behavior == current && result.getClickResult() instanceof ClickResult.RightClickResult click
                && click.getType() == ClickResult.RightClickResult.RightClickType.USE_ITEM) {
                current.onFireworkUsed();
            }
        });
    }

    public void landingSpotIsBad(BlockPos endPos) {
        badLandingSpots.add(endPos);
        goingToLandingSpot = false;
        this.landingSpot = null;
        this.landingSearchState = null;
        this.state = State.FLYING;
    }

    private void destroyBehaviorAsync() {
        var behavior = this.behavior;
        if (behavior != null) {
            this.behavior = null;
            behavior.cancel();
            Baritone.getExecutor().execute(() -> {
                behavior.destroy();
            });
        }
    }

    @Override
    public double priority() {
        return 0; // higher priority than CustomGoalProcess
    }

    @Override
    public String displayName0() {
        return "Elytra - " + this.state.description;
    }

    public void repackChunks() {
        var client = Proxy.getInstance().getClient();
        if (client == null) return;
        if (!client.getClientEventLoop().inEventLoop()) {
            client.getClientEventLoop().execute(this::repackChunks);
            return;
        }
        if (this.npfContext == null) return;

        for (var chunk : CACHE.getChunkCache().getCache().values()) {
            npfContext.queueForPacking(chunk);
        }
    }

    public BlockPos currentDestination() {
        var current = this.behavior;
        return current != null ? current.destination : null;
    }

    public List<BlockPos> getPath() {
        var current = this.behavior;
        return current != null ? current.pathManager.getPath() : Collections.emptyList();
    }

    public int getPathPosition() {
        var current = behavior;
        return current == null ? 0 : current.pathManager.getNear();
    }

    public PathingRequestFuture pathTo(BlockPos destination) {
        return pathTo(destination, new GoalBlock(destination));
    }

    private PathingRequestFuture pathTo(BlockPos destination, Goal goal) {
        if (!isSupportedPos(destination)) {
            throw new IllegalArgumentException("The goal must be within bounds to use elytra flight.");
        }

        if (ctx.player() != null && !isSupportedPos(ctx.playerFeet())) {
            throw new IllegalArgumentException("The player must be within bounds to use elytra flight.");
        }

        var client = Proxy.getInstance().getClient();
        if (client == null || !client.isOnline()) throw new IllegalStateException("Not connected to a world");
        var eventLoop = client.getClientEventLoop();
        if (eventLoop.inEventLoop()) {
            return startPath(destination, goal);
        } else {
            return eventLoop.submit(() -> startPath(destination, goal)).syncUninterruptibly().getNow();
        }
    }

    private synchronized PathingRequestFuture startPath(BlockPos destination, Goal goal) {
        if (this.future != null && !this.future.isCompleted()) this.future.complete(false);
        var request = new PathingRequestFuture();
        request.setGoal(goal);
        this.future = request;
        this.emergencyLanding = false;
        try {
            this.pathTo0(destination, false);
        } catch (RuntimeException e) {
            this.onLostControl();
            throw e;
        }
        return request;
    }

    private synchronized void pathTo0(BlockPos destination, boolean appendDestination) {
        this.onLostControl(false);
        if (!appendDestination) this.badLandingSpots.clear();
        this.behavior = new ElytraBehavior(this.baritone, this, getNpfContext(), destination, appendDestination);

        if (CACHE.getChunkCache().getCurrentDimension() != null) {
            this.repackChunks();
        }
        this.behavior.pathTo();
    }

    public PathingRequestFuture pathTo(Goal iGoal) {
        final int x;
        final int y;
        final int z;
        if (iGoal instanceof GoalXZ) {
            var goal = (GoalXZ) iGoal;
            x = goal.x();
            // ElytraBehavior will automatically change the destination height depending on if we're above or below the roof
            y = 64;
            z = goal.z();
        } else if (iGoal instanceof GoalBlock) {
            var goal = (GoalBlock) iGoal;
            x = goal.x();
            y = goal.y();
            z = goal.z();
        } else {
            throw new IllegalArgumentException("The goal must be a GoalXZ or GoalBlock");
        }

        return this.pathTo(new BlockPos(x, y, z), iGoal);
    }

    private boolean isSupportedPos(BlockPos pos) {
        final boolean isNether = World.getCurrentDimension() == DimensionRegistry.THE_NETHER.get();
        final int minY = World.getCurrentDimension().minY();
        final int maxY = (isNether && !false /* Baritone.settings().elytraAllowAboveRoof */) ? 127 : Math.min(minY + 384, World.getCurrentDimension().height() + minY);

        final boolean aboveRoof = false /* Baritone.settings().elytraAllowAboveRoof */;
        final boolean aboveBuild = true /* Baritone.settings().elytraAllowAboveBuildLimit */;

        final boolean enforceMaxY = isNether ? !(aboveRoof && aboveBuild) : !aboveBuild;

        if (pos.y() < minY) {
            return false;
        }

        return !enforceMaxY || pos.y() < maxY;
    }

    private boolean shouldLandForSafety() {
        var chest = CACHE.getPlayerCache().getEquipment(EquipmentSlot.CHESTPLATE);
        if ((chest == null || chest.getId() != ItemRegistry.ELYTRA.id()) || ItemUtil.getDamageUntilBreak(chest) < 5 /* Baritone.settings().elytraMinimumDurability */) {
            // elytrabehavior replaces when durability <= minimumDurability, so if durability < minimumDurability then we can reasonably assume that the elytra will soon be broken without replacement
            return true;
        }

        var inv = CACHE.getPlayerCache().getPlayerInventory();
        int qty = 0;
        for (int i = 9; i < 46; i++) {
            if (ElytraBehavior.isFireworks(inv.get(i))) {
                qty += inv.get(i).getAmount();
            }
        }
        if (qty <= 5 /* Baritone.settings().elytraMinFireworksBeforeLanding */) {
            return true;
        }
        return false;
    }

    public boolean isLoaded() {
        return true;
    }

    public boolean isSafeToCancel() {
        return !this.isActive() || !(this.state == State.FLYING || this.state == State.START_FLYING);
    }

    public enum State {
        JUMP("Takeoff"),
        PAUSE("Waiting for elytra path"),
        START_FLYING("Begin flying"),
        FLYING("Flying"),
        LANDING("Landing");

        public final String description;

        State(String desc) {
            this.description = desc;
        }
    }

    public synchronized void onPostTick() {
        var procThisTick = baritone.getPathingControlManager().mostRecentInControl().orElse(null);
        if (this.behavior != null && procThisTick == this) this.behavior.onPostTick();
    }

    public synchronized void onPlayerPosRotate() {
        if (this.behavior != null) this.behavior.onReceivePacket();
    }

    public synchronized void onChunkEvent(Chunk chunk) {
        if (this.npfContext != null) this.npfContext.queueForPacking(chunk);
    }

    public synchronized void onBlockChange(int x, int y, int z, int stateId) {
        if (this.npfContext != null) this.npfContext.queueBlockUpdate(x, y, z, stateId);
    }

    public synchronized void onChunkUnload(int x, int z) {
        if (this.npfContext != null) this.npfContext.queueChunkUnload(x, z);
    }

    private static boolean isInBounds(BlockPos pos) {
        var dim = World.getCurrentDimension();
        int maxY = (dim == DimensionRegistry.THE_NETHER.get() && !false /* Baritone.settings().elytraAllowAboveRoof */)
            ? 127 : Math.min(dim.minY() + 384, dim.buildHeight());
        return pos.y() >= dim.minY() && pos.y() < maxY;
    }

    private boolean isSafeBlock(Block block) {
        return block == BlockRegistry.NETHERRACK || block == BlockRegistry.GRAVEL || block == BlockRegistry.SOUL_SAND || block == BlockRegistry.SOUL_SOIL || (block == BlockRegistry.NETHER_BRICKS && false /* Baritone.settings().elytraAllowLandOnNetherFortress */)
                || block == BlockRegistry.STONE || block == BlockRegistry.DEEPSLATE || block == BlockRegistry.GRASS_BLOCK || block == BlockRegistry.SAND || block == BlockRegistry.RED_SAND || block == BlockRegistry.TERRACOTTA
                || block == BlockRegistry.SNOW || block == BlockRegistry.ICE || block == BlockRegistry.MYCELIUM || block == BlockRegistry.PODZOL
                || block == BlockRegistry.DARK_OAK_LEAVES || block == BlockRegistry.JUNGLE_LEAVES
                || block == BlockRegistry.END_STONE || block == BlockRegistry.BEDROCK
                || block == BlockRegistry.OBSIDIAN || block == BlockRegistry.COBBLESTONE;
    }

    private boolean isSafeBlock(BlockPos pos) {
        return isSafeBlock(World.getBlock(pos));
    }

    private boolean isAtEdge(BlockPos pos) {
        return !isSafeBlock(pos.north())
                || !isSafeBlock(pos.south())
                || !isSafeBlock(pos.east())
                || !isSafeBlock(pos.west())
                // corners
                || !isSafeBlock(pos.north().west())
                || !isSafeBlock(pos.north().east())
                || !isSafeBlock(pos.south().west())
                || !isSafeBlock(pos.south().east());
    }

    private boolean isColumnAir(BlockPos landingSpot, int minHeight) {
        var mut = landingSpot;
        final int maxY = mut.y() + minHeight;
        for (int y = mut.y() + 1; y <= maxY; y++) {
            mut = new BlockPos(mut.x(), y, mut.z());
            if (!World.isChunkLoadedBlockPos(mut.x(), mut.z()) || !World.getBlock(mut).isAir()) {
                return false;
            }
        }
        return true;
    }

    private boolean hasAirBubble(BlockPos pos) {
        final int radius = 4; // Half of the full width, rounded down, as we're counting blocks in each direction from the center
        var mut = BlockPos.ZERO;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    mut = new BlockPos(pos.x() + x, pos.y() + y, pos.z() + z);
                    if (!World.isChunkLoadedBlockPos(mut.x(), mut.z()) || !World.getBlock(mut).isAir()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private BlockPos checkLandingSpot(BlockPos pos, LongOpenHashSet checkedSpots) {
        var mut = pos;
        while (mut.y() >= World.getCurrentDimension().minY()) {
            if (checkedSpots.contains(mut.asLong())) {
                return null;
            }
            checkedSpots.add(mut.asLong());
            var block = World.getBlock(mut);

            if (isSafeBlock(block)) {
                if (!isAtEdge(mut)) {
                    return new BlockPos(mut);
                }
                return null;
            } else if (block != BlockRegistry.AIR) {
                return null;
            }
            mut = new BlockPos(mut.x(), mut.y() - 1, mut.z());
        }
        return null; // void
    }

    private BlockPos findSafeLandingSpot(BlockPos start) {
        final boolean useHeightmap = ctx.player().getY() > World.getSurfaceHeight(start.x(), start.z());
        if (this.landingSearchState == null || !this.landingSearchState.isCompatible(start, useHeightmap)) {
            this.landingSearchState = new LandingSearchState(start, this.behavior.destination, useHeightmap);
        } else {
            this.landingSearchState.updateStartPosition(start);
        }

        var landingSpot = this.landingSearchState.advance();
        if (landingSpot != null || this.landingSearchState.exhausted) {
            this.landingSearchState = null;
        }
        return landingSpot;
    }

    private boolean isChunkLoaded(BlockPos pos) {
        return World.isChunkLoadedBlockPos(pos.x(), pos.z());
    }

    private final class LandingSearchState {
        private final BlockPos origin;
        private final boolean useHeightmap;
        private final Queue<BlockPos> queue;
        private final Set<BlockPos> visited = new HashSet<>();
        private final LongOpenHashSet checkedPositions = new LongOpenHashSet();
        private boolean exhausted;

        private LandingSearchState(BlockPos origin, BlockPos dest, boolean useHeightmap) {
            this.origin = origin;
            this.useHeightmap = useHeightmap;

            var target = isChunkLoaded(dest) ? dest : origin;
            this.queue = new PriorityQueue<>(Comparator.<BlockPos>comparingInt(pos -> (pos.x() - target.x()) * (pos.x() - target.x()) + (pos.z() - target.z()) * (pos.z() - target.z())).thenComparingInt(pos -> -pos.y()));
            this.queue.add(target);
        }

        private boolean isCompatible(BlockPos start, boolean useHeightmap) {
            // Restart if we've moved more than a chunk so the priority adjusts and newly loaded chunks get revisited
            return this.useHeightmap == useHeightmap && this.origin.squaredDistance(start) <= (16 * 16);
        }

        private void updateStartPosition(BlockPos start) {
            if (this.visited.add(start)) {
                this.queue.add(start);
            }
        }

        private BlockPos advance() {
            final long deadline = System.nanoTime() + LANDING_SEARCH_BUDGET_NANOS;
            while (!this.queue.isEmpty()) {
                if (System.nanoTime() >= deadline) {
                    return null;
                }
                var qPos = this.queue.poll();
                if (!isChunkLoaded(qPos)) {
                    continue;
                }
                var landing = this.useHeightmap ? this.advanceHeightmap(qPos) : this.advanceUnderground(qPos);
                if (landing != null) {
                    return landing;
                }
            }
            this.exhausted = true;
            return null;
        }

        private BlockPos advanceUnderground(BlockPos pos) {
            if (isInBounds(pos) && World.getBlock(pos) == BlockRegistry.AIR) {
                var actualLandingSpot = checkLandingSpot(pos, this.checkedPositions);
                if (actualLandingSpot != null) {
                    landingColumnHeight = SHORT_LANDING_COLUMN_HEIGHT;
                    if (isColumnAir(actualLandingSpot, landingColumnHeight) && hasAirBubble(actualLandingSpot.above(landingColumnHeight)) && !badLandingSpots.contains(actualLandingSpot.above(landingColumnHeight))) {
                        return actualLandingSpot.above(landingColumnHeight);
                    }
                }
                if (this.visited.add(pos.north())) this.queue.add(pos.north());
                if (this.visited.add(pos.east())) this.queue.add(pos.east());
                if (this.visited.add(pos.south())) this.queue.add(pos.south());
                if (this.visited.add(pos.west())) this.queue.add(pos.west());
                if (this.visited.add(pos.above())) this.queue.add(pos.above());
                if (this.visited.add(pos.below())) this.queue.add(pos.below());
            }
            return null;
        }

        private BlockPos advanceHeightmap(BlockPos qPos) {
            int height = World.getSurfaceHeight(qPos.x(), qPos.z());
            var pos = new BlockPos(qPos.x(), height + 1, qPos.z());
            if (isInBounds(pos) && World.getBlock(pos) == BlockRegistry.AIR) {
                var actualLandingSpot = checkLandingSpot(pos, this.checkedPositions);
                if (actualLandingSpot != null) {
                    landingColumnHeight = ctx.playerFeet().y() - actualLandingSpot.y() < LONG_LANDING_COLUMN_HEIGHT ? SHORT_LANDING_COLUMN_HEIGHT : LONG_LANDING_COLUMN_HEIGHT;
                    if (hasAirBubble(actualLandingSpot.above(landingColumnHeight)) && !badLandingSpots.contains(actualLandingSpot.above(landingColumnHeight))) {
                        return actualLandingSpot.above(landingColumnHeight);
                    }
                }
                if (this.visited.add(pos.north())) this.queue.add(pos.north());
                if (this.visited.add(pos.east())) this.queue.add(pos.east());
                if (this.visited.add(pos.south())) this.queue.add(pos.south());
                if (this.visited.add(pos.west())) this.queue.add(pos.west());
            }
            return null;
        }
    }

    private NetherPathfinderContext getNpfContext() {
        if(this.npfContext == null) {
            this.npfContext = new NetherPathfinderContext();
        }
        return this.npfContext;
    }

    private void destroyNpfContextAsync() {
        var npf = this.npfContext;
        if (npf != null) {
            this.npfContext = null;
            npf.cancel();
            Baritone.getExecutor().execute(npf::destroy);
        }
    }
}
