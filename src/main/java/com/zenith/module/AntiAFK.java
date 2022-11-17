package com.zenith.module;

import com.collarmc.pounce.Subscribe;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerSwingArmPacket;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zenith.Proxy;
import com.zenith.event.module.AntiAfkStuckEvent;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.pathing.BlockPos;
import com.zenith.pathing.Pathing;
import com.zenith.pathing.Position;
import com.zenith.util.TickTimer;
import org.apache.commons.collections4.iterators.LoopingListIterator;
import org.apache.commons.math3.util.Pair;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.zenith.util.Constants.*;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;

public class AntiAFK extends Module {
    private final TickTimer swingTickTimer = new TickTimer();
    private final TickTimer startWalkTickTimer = new TickTimer();
    private final TickTimer rotateTimer = new TickTimer();
    private static final long positionCacheTTLMins = 20;
    private final TickTimer distanceDeltaCheckTimer = new TickTimer();
    private final Pathing pathing;
    private boolean shouldWalk = false;
    private final Cache<Position, Position> positionCache;
    private final List<Pair<Integer, Integer>> walkDirections = asList(
            new Pair<>(1, 0), new Pair<>(-1, 0),
            new Pair<>(1, 1), new Pair<>(-1, -1),
            new Pair<>(0, -1), new Pair<>(0, 1),
            new Pair<>(-1, 1), new Pair<>(1, -1),
            new Pair<>(-1, 0), new Pair<>(1, 0),
            new Pair<>(1, -1), new Pair<>(-1, 1),
            new Pair<>(0, 1), new Pair<>(0, -1));
    private Instant lastDistanceDeltaWarningTime = Instant.EPOCH;
    private final LoopingListIterator<Pair<Integer, Integer>> walkDirectionIterator = new LoopingListIterator<>(walkDirections);
    private BlockPos currentPathingGoal;
    // tick time since we started falling
    // can be negative, indicates pathing should wait until it reaches 0 to fall
    private int gravityT = 0;

    public AntiAFK(Proxy proxy, final Pathing pathing) {
        super(proxy);
        this.pathing = pathing;
        this.positionCache = CacheBuilder.newBuilder()
                .expireAfterWrite(positionCacheTTLMins, TimeUnit.MINUTES)
                .build();
    }

    @Subscribe
    public void handleClientTickEvent(final ClientTickEvent event) {
        if (CONFIG.client.extra.antiafk.enabled && isNull(this.proxy.getCurrentPlayer().get()) && !proxy.isInQueue() && CACHE.getPlayerCache().getThePlayer().getHealth() > 0) {
            if (CONFIG.client.extra.antiafk.actions.swingHand) {
                swingTick();
            }
            if (CONFIG.client.extra.antiafk.actions.gravity) {
                gravity();
            }
            if (CONFIG.client.extra.antiafk.actions.walk && (!CONFIG.client.extra.antiafk.actions.gravity || gravityT <= 0)) {
                walkTick();
                // check distance delta every 10 mins. Stuck kick should happen at 30 mins
                if (distanceDeltaCheckTimer.tick(12000L, true) && CONFIG.client.server.address.toLowerCase().contains("2b2t.org") && CONFIG.client.extra.antiafk.actions.stuckWarning) {
                    final double distanceMovedDelta = getDistanceMovedDelta();
                    if (distanceMovedDelta < 6) {
                        MODULE_LOG.warn("AntiAFK appears to be stuck. Distance moved: {}", distanceMovedDelta);
                        if (Instant.now().minus(Duration.ofMinutes(20)).isAfter(lastDistanceDeltaWarningTime)) {
                            // only send discord warning once every 20 mins so we don't spam too hard
                            EVENT_BUS.dispatch(new AntiAfkStuckEvent(distanceMovedDelta));
                            lastDistanceDeltaWarningTime = Instant.now();
                        }
                    }
                }
            }
            if (CONFIG.client.extra.antiafk.actions.rotate && (!CONFIG.client.extra.spook.enabled || !spookHasTarget())) {
                rotateTick();
            }
        }
    }

    @Override
    public void clientTickStarting() {
        reset();
    }

    private void reset() {
        swingTickTimer.reset();
        startWalkTickTimer.reset();
        rotateTimer.reset();
        distanceDeltaCheckTimer.reset();
        shouldWalk = false;
        positionCache.invalidateAll();
        lastDistanceDeltaWarningTime = Instant.EPOCH;
        walkDirectionIterator.reset();
        currentPathingGoal = null;
        gravityT = 0;
    }

    private boolean spookHasTarget() {
        return this.proxy.getModuleManager().getModule(Spook.class)
                .map(m -> ((Spook) m).hasTarget.get())
                .orElse(false);
    }

    private double getDistanceMovedDelta() {
        final Collection<Position> positions = this.positionCache.asMap().values();
        if (positions.size() < 10) {
            // handle race condition where we check delta right after login
            // this means we're asserting that we've had a significant amount of movements
            // until then we're not giving a true delta, there might be a better way to handle the race condition
            return Double.MAX_VALUE;
        }
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
        if (rotateTimer.tick(1500L, true)) {
            this.proxy.getClient().send(new ClientPlayerRotationPacket(
                    true,
                    -90 + (90 + 90) * ThreadLocalRandom.current().nextFloat(),
                    -90 + (90 + 90) * ThreadLocalRandom.current().nextFloat()
            ));
        }
    }

    public void handlePlayerPosRotate() {
        synchronized (this) {
            this.gravityT = -2;
        }
    }

    private void walkTick() {
        if (startWalkTickTimer.tick(400L, true)) {
            shouldWalk = true;
            final Pair<Integer, Integer> directions = walkDirectionIterator.next();
            currentPathingGoal = pathing.getCurrentPlayerPos()
                    .addX(CONFIG.client.extra.antiafk.actions.walkDistance * directions.getKey())
                    .addZ(CONFIG.client.extra.antiafk.actions.walkDistance * directions.getValue())
                    .toBlockPos();

        }
        if (shouldWalk) {
            if (reachedPathingGoal()) {
                shouldWalk = false;
            } else {
                Position nextMovePos = pathing.calculateNextMove(currentPathingGoal);
                if (nextMovePos.equals(pathing.getCurrentPlayerPos())) {
                    shouldWalk = false;
                }
                this.proxy.getClient().send(nextMovePos.toPlayerPositionPacket());
                this.positionCache.put(nextMovePos, nextMovePos);
            }
        }
    }

    private boolean reachedPathingGoal() {
        return Objects.equals(pathing.getCurrentPlayerPos().toBlockPos(), currentPathingGoal);
    }

    private void gravity() {
        synchronized (this) {
            final Optional<Position> nextGravityMove = pathing.calculateNextGravityMove(gravityT);
            if (nextGravityMove.isPresent()) {
                if (!nextGravityMove.get().equals(pathing.getCurrentPlayerPos())) {
                    this.proxy.getClient().send(nextGravityMove.get().toPlayerPositionPacket());
                }
                gravityT++;
            } else {
                gravityT = 0;
            }
        }
    }

    private void swingTick() {
        if (swingTickTimer.tick(3000L, true)) {
            this.proxy.getClient().send(new ClientPlayerSwingArmPacket(Hand.MAIN_HAND));
        }
    }
}
