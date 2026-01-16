package com.zenith.feature.pathfinder;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zenith.cache.data.entity.EntityLiving;
import com.zenith.event.client.ClientBotTick;
import com.zenith.feature.pathfinder.behavior.InventoryBehavior;
import com.zenith.feature.pathfinder.behavior.LookBehavior;
import com.zenith.feature.pathfinder.behavior.PathingBehavior;
import com.zenith.feature.pathfinder.goals.Goal;
import com.zenith.feature.pathfinder.goals.GoalBlock;
import com.zenith.feature.pathfinder.goals.GoalXZ;
import com.zenith.feature.pathfinder.process.*;
import com.zenith.feature.player.InputRequest;
import com.zenith.mc.block.Block;
import com.zenith.mc.block.BlockPos;
import com.zenith.mc.item.ItemData;
import com.zenith.util.math.MathHelper;
import com.zenith.util.timer.Timer;
import com.zenith.util.timer.Timers;
import lombok.Data;
import lombok.Getter;
import org.cloudburstmc.math.vector.Vector3d;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;

/**
 *
 * todo:
 *  doors, fences, and gates opening interactions
 *  interface for dispatching pathing commands with configurations
 *      i.e. disallow block breaking for certain goals, allow long distance falling, etc
 *  Rethink the baritone "Process" system. is there a better abstraction for multi-step goals?
 */

@Data
public class Baritone implements Pathfinder {
    public static final int POST_TICK_PRIORITY = -40000;
    private final PathingBehavior pathingBehavior = new PathingBehavior(this);
    private final InputOverrideHandler inputOverrideHandler = new InputOverrideHandler(this);
    private final LookBehavior lookBehavior = new LookBehavior(this);
    private final InventoryBehavior inventoryBehavior = new InventoryBehavior(this);
    private final PlayerContext playerContext = PlayerContext.INSTANCE;
    @Getter private static final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder()
            .setNameFormat("Baritone")
            .setDaemon(true)
            .setUncaughtExceptionHandler((t, e) -> PATH_LOG.error("Error in Baritone thread", e))
            .build()));
    private final PathingControlManager pathingControlManager = new PathingControlManager(this);
    private final CustomGoalProcess customGoalProcess = new CustomGoalProcess(this);
    private final FollowProcess followProcess = new FollowProcess(this);
    private final GetToBlockProcess getToBlockProcess = new GetToBlockProcess(this);
    private final MineProcess mineProcess = new MineProcess(this);
    private final InteractWithProcess interactWithProcess = new InteractWithProcess(this);
    private final ClearAreaProcess clearAreaProcess = new ClearAreaProcess(this);
    private final Timer teleportDelayTimer = Timers.timer();
    private final IngamePathRenderer ingamePathRenderer = new IngamePathRenderer();

    public Baritone() {
        pathingControlManager.registerProcess(customGoalProcess);
        pathingControlManager.registerProcess(followProcess);
        pathingControlManager.registerProcess(getToBlockProcess);
        pathingControlManager.registerProcess(mineProcess);
        pathingControlManager.registerProcess(interactWithProcess);
        pathingControlManager.registerProcess(clearAreaProcess);
        EVENT_BUS.subscribe(
            this,
            of(ClientBotTick.class, this::onClientBotTick),
            of(ClientBotTick.class, POST_TICK_PRIORITY, this::onClientBotTickPost),
            of(ClientBotTick.Starting.class, this::onClientBotTickStarting),
            of(ClientBotTick.Stopped.class, this::onClientBotTickStopped)
        );
    }

    public static int getPriority() {
        return Objects.requireNonNullElse(CONFIG.client.extra.pathfinder.priority, 7000);
    }

    public boolean isActive() {
        return getPathingBehavior().getGoal() != null || getPathingControlManager().isActive();
    }

    public boolean isGoalActive(@NonNull Goal goal) {
        Goal activeGoal = getPathingBehavior().getGoal();
        return activeGoal != null && activeGoal.equals(goal);
    }

    @Override
    public PathingRequestFuture pathTo(int x, int z) {
        return pathTo(new GoalXZ(x, z));
    }

    @Override
    public PathingRequestFuture pathTo(int x, int y, int z) {
        return pathTo(new GoalBlock(x, y, z));
    }

    @Override
    public PathingRequestFuture pathTo(@NonNull Goal goal) {
        return getCustomGoalProcess().setGoalAndPath(goal);
    }

    @Override
    public PathingRequestFuture thisWay(final int dist) {
        Vector3d vector3d = MathHelper.calculateRayEndPos(
            CACHE.getPlayerCache().getX(),
            CACHE.getPlayerCache().getY(),
            CACHE.getPlayerCache().getZ(),
            CACHE.getPlayerCache().getYaw(),
            0,
            dist
        );
        return pathTo(MathHelper.floorI(vector3d.getX()), MathHelper.floorI(vector3d.getZ()));
    }

    @Override
    public PathingRequestFuture getTo(final Block block) {
        return getGetToBlockProcess().getToBlock(block);
    }

    @Override
    public PathingRequestFuture getTo(final Block block, boolean rightClickContainerOnArrival) {
        return getGetToBlockProcess().getToBlock(block, rightClickContainerOnArrival);
    }

    @Override
    public PathingRequestFuture mine(Block... blocks) {
        return getMineProcess().mine(blocks);
    }

    @Override
    public PathingRequestFuture follow(Predicate<EntityLiving> entityPredicate) {
        return getFollowProcess().follow(entityPredicate);
    }

    @Override
    public PathingRequestFuture follow(EntityLiving target) {
        return getFollowProcess().follow(target);
    }

    @Override
    public PathingRequestFuture pickup(final ItemData... items) {
        return getFollowProcess().pickup(items);
    }

    @Override
    public PathingRequestFuture pickup() {
        return getFollowProcess().pickup();
    }

    @Override
    public PathingRequestFuture leftClickBlock(int x, int y, int z) {
        return getInteractWithProcess().leftClickBlock(x, y, z);
    }

    @Override
    public PathingRequestFuture rightClickBlock(int x, int y, int z) {
        return getInteractWithProcess().rightClickBlock(x, y, z);
    }

    @Override
    public PathingRequestFuture breakBlock(int x, int y, int z, boolean autoTool) {
        return getInteractWithProcess().breakBlock(x, y, z, autoTool);
    }

    /**
     * API may change or be removed in future updates
     */
    @ApiStatus.Experimental
    @Override
    public PathingRequestFuture placeBlock(int x, int y, int z, ItemData placeItem) {
        return getInteractWithProcess().placeBlock(x, y, z, placeItem);
    }

    @Override
    public PathingRequestFuture leftClickEntity(EntityLiving entity) {
        return getInteractWithProcess().leftClickEntity(entity);
    }

    @Override
    public PathingRequestFuture rightClickEntity(EntityLiving entity) {
        return getInteractWithProcess().rightClickEntity(entity);
    }

    @Override
    public PathingRequestFuture clearArea(BlockPos pos1, BlockPos pos2) {
        return getClearAreaProcess().clearArea(pos1, pos2);
    }

    @Override
    public void stop() {
        getPathingBehavior().cancelEverything();
    }

    @Override
    public @Nullable Goal currentGoal() {
        return pathingBehavior.getGoal();
    }

    private void onClientBotTick(ClientBotTick event) {
        if (!CACHE.getPlayerCache().isAlive()) return;
        if (CACHE.getChunkCache().getCache().size() < 8) return;
        if (!teleportDelayTimer.tick(CONFIG.client.extra.pathfinder.teleportDelayMs, false)) return;
        lookBehavior.onTick();
        pathingBehavior.onTick();
        if (pathingControlManager.isActive()) {
            inventoryBehavior.onTick();
        }
        inputOverrideHandler.onTick();
        ingamePathRenderer.onTick();

        if (pathingBehavior.isPathing() || (pathingControlManager.isActive() && lookBehavior.currentRotation != null)) {
            var rotation = lookBehavior.currentRotation;
            var req = InputRequest.builder()
                .owner(this)
                .input(inputOverrideHandler.currentInput)
                .priority(getPriority());
            if (rotation != null) {
                req
                    .yaw(rotation.yaw())
                    .pitch(rotation.pitch());
            }
            INPUTS.submit(req.build());
        }
    }

    private void onClientBotTickPost(ClientBotTick event) {
        pathingControlManager.postTick();
    }

    private void onClientBotTickStopped(ClientBotTick.Stopped event) {
        getPathingBehavior().cancelEverything();
    }

    private void onClientBotTickStarting(ClientBotTick.Starting event) {
        getPathingBehavior().cancelEverything();
    }

    public void onPlayerPosRotate() {
        teleportDelayTimer.reset();
    }
}
