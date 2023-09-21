package com.zenith.module.impl;

import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerState;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Iterators;
import com.zenith.Proxy;
import com.zenith.event.Subscription;
import com.zenith.event.module.AntiAfkStuckEvent;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.event.proxy.DeathEvent;
import com.zenith.feature.pathing.BlockPos;
import com.zenith.feature.pathing.Pathing;
import com.zenith.feature.pathing.Position;
import com.zenith.module.Module;
import com.zenith.util.TickTimer;
import com.zenith.util.math.MathHelper;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.zenith.Shared.*;
import static com.zenith.event.SimpleEventBus.pair;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;

public class AntiAFK extends Module {
    private final TickTimer swingTickTimer = new TickTimer();
    private final TickTimer startWalkTickTimer = new TickTimer();
    private final TickTimer rotateTimer = new TickTimer();
    private static final long positionCacheTTLMins = 20;
    private final TickTimer distanceDeltaCheckTimer = new TickTimer();
    private boolean shouldWalk = false;
    private final Cache<Position, Position> positionCache;
    private final List<WalkDirection> walkDirections = asList(
            new WalkDirection(1, 0), new WalkDirection(-1, 0),
            new WalkDirection(1, 1), new WalkDirection(-1, -1),
            new WalkDirection(0, -1), new WalkDirection(0, 1),
            new WalkDirection(-1, 1), new WalkDirection(1, -1),
            new WalkDirection(-1, 0), new WalkDirection(1, 0),
            new WalkDirection(1, -1), new WalkDirection(-1, 1),
            new WalkDirection(0, 1), new WalkDirection(0, -1)
    );
    private Instant lastDistanceDeltaWarningTime = Instant.EPOCH;
    private boolean stuck = false;
    private final Iterator<WalkDirection> walkDirectionIterator = Iterators.cycle(walkDirections);
    private BlockPos currentPathingGoal;
    private int antiStuckT = 0;
    private double antiStuckStartY = 0;
    public static final int MOVEMENT_PRIORITY = 100;

    public AntiAFK() {
        super();
        this.positionCache = CacheBuilder.newBuilder()
                .expireAfterWrite(positionCacheTTLMins, TimeUnit.MINUTES)
                .build();
    }

    @Override
    public Subscription subscribeEvents() {
        return EVENT_BUS.subscribe(
            pair(ClientTickEvent.class, this::handleClientTickEvent),
            pair(DeathEvent.class, this::handleDeathEvent)
        );
    }

    @Override
    public Supplier<Boolean> shouldBeEnabled() {
        return () -> CONFIG.client.extra.antiafk.enabled;
    }

    public void handleClientTickEvent(final ClientTickEvent event) {
        if (Proxy.getInstance().isConnected()
                && isNull(Proxy.getInstance().getCurrentPlayer().get())
                && !Proxy.getInstance().isInQueue()
                && CACHE.getPlayerCache().getThePlayer().getHealth() > 0
                && MODULE_MANAGER.getModule(KillAura.class).map(ka -> !ka.isActive()).orElse(true)) {
            if (CONFIG.client.extra.antiafk.actions.swingHand) {
                swingTick();
            }

//            if (CONFIG.client.extra.antiafk.actions.antiStuck && isStuck()) {
//                if (antiStuckT < 0) {
//                    antiStuckT++;
//                } else {
//                    if (antiStuckTick()) {
//                        return;
//                    }
//                }
//            }

            if (CONFIG.client.extra.antiafk.actions.rotate && (!CONFIG.client.extra.spook.enabled || !spookHasTarget())) {
                rotateTick();
            }
            if (CONFIG.client.extra.antiafk.actions.walk) {
                walkTick();
                Position currentPlayerPos = Pathing.getCurrentPlayerPos();
                this.positionCache.put(currentPlayerPos, currentPlayerPos);
                // check distance delta every 9 mins. Stuck kick should happen at 20 mins
                if (distanceDeltaCheckTimer.tick(10800L, true) && CONFIG.client.server.address.toLowerCase().contains("2b2t.org") && CONFIG.client.extra.antiafk.actions.stuckWarning) {
                    final double distanceMovedDelta = getDistanceMovedDelta();
                    if (distanceMovedDelta < 6) {
                        MODULE_LOG.warn("AntiAFK appears to be stuck. Distance moved: {}", distanceMovedDelta);
                        stuck = true;
                        if (Instant.now().minus(Duration.ofMinutes(20)).isAfter(lastDistanceDeltaWarningTime)) {
                            // only send discord warning once every 20 mins so we don't spam too hard
                            EVENT_BUS.postAsync(new AntiAfkStuckEvent(distanceMovedDelta));
                            lastDistanceDeltaWarningTime = Instant.now();
                        }
                    } else {
                        stuck = false;
                    }
                }
            }

        }
    }

    public void handleDeathEvent(final DeathEvent event) {
        synchronized (this) {
            reset();
        }
    }

    @Override
    public void clientTickStarting() {
        reset();
        sendClientPacketAsync(new ServerboundPlayerCommandPacket(CACHE.getPlayerCache().getEntityId(), PlayerState.STOP_SPRINTING));
    }

    @Override
    public void clientTickStopping() {
        reset();
    }

    private void reset() {
        synchronized (this) {
            swingTickTimer.reset();
            startWalkTickTimer.reset();
            rotateTimer.reset();
            distanceDeltaCheckTimer.reset();
            shouldWalk = false;
            positionCache.invalidateAll();
            lastDistanceDeltaWarningTime = Instant.EPOCH;
            currentPathingGoal = null;
            antiStuckT = 0;
            stuck = false;
        }
    }

    private boolean spookHasTarget() {
        return MODULE_MANAGER.getModule(Spook.class)
                .map(m -> m.hasTarget.get())
                .orElse(false);
    }

    private double getDistanceMovedDelta() {
        final Collection<Position> positions = this.positionCache.asMap().values();
        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        for (Position pos : positions) {
            minX = Math.min(pos.getX(), minX);
            maxX = Math.max(pos.getX(), maxX);
            minZ = Math.min(pos.getZ(), minZ);
            maxZ = Math.max(pos.getZ(), maxZ);
        }
        return Math.max(Math.abs(maxX - minX), Math.abs(maxZ - minZ));
    }

    private void rotateTick() {
        if (rotateTimer.tick(300L, true)) {
            PATHING.rotate(
                -180 + (360 * ThreadLocalRandom.current().nextFloat()),
                -90 + (180 * ThreadLocalRandom.current().nextFloat()),
                MOVEMENT_PRIORITY - 1
            );
        }
    }

    public void handlePlayerPosRotate() {
        synchronized (this) {
            this.antiStuckT = -2;
            this.shouldWalk = false;
        }
    }

    private void walkTick() {
        if (startWalkTickTimer.tick(400L, true)) {
            shouldWalk = true;
            final WalkDirection directions = walkDirectionIterator.next();
            currentPathingGoal = Pathing.getCurrentPlayerPos()
                    .addX(CONFIG.client.extra.antiafk.actions.walkDistance * directions.from)
                    .addZ(CONFIG.client.extra.antiafk.actions.walkDistance * directions.to)
                    .toBlockPos();
        }
        if (shouldWalk) {
            if (reachedPathingGoal()) {
                shouldWalk = false;
            } else {
                PATHING.moveRotTowardsBlockPos(MathHelper.floorToInt(currentPathingGoal.getX()), MathHelper.floorToInt(currentPathingGoal.getZ()), MOVEMENT_PRIORITY);
            }
        }
    }

    private boolean reachedPathingGoal() {
        final int px = MathHelper.floorToInt(Pathing.getCurrentPlayerPos().getX());
        final int pz = MathHelper.floorToInt(Pathing.getCurrentPlayerPos().getZ());
        return px == currentPathingGoal.getX() && pz == currentPathingGoal.getZ();
    }

    private void swingTick() {
        if (swingTickTimer.tick(3000L, true)) {
            // todo: move this to PlayerSimulation and assign priority so it doesn't conflict with other modules
            sendClientPacketAsync(new ServerboundSwingPacket(Hand.MAIN_HAND));
        }
    }

    public boolean isStuck() {
        return this.stuck;
    }


    record WalkDirection(int from, int to) { }
}
