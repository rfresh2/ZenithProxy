package com.zenith.module.impl;

import com.github.rfresh2.EventConsumer;
import com.google.common.collect.Iterators;
import com.zenith.event.client.ClientBotTick;
import com.zenith.event.client.ClientDeathEvent;
import com.zenith.feature.player.*;
import com.zenith.mc.block.BlockPos;
import com.zenith.module.api.Module;
import com.zenith.util.math.MathHelper;
import com.zenith.util.timer.Timer;
import com.zenith.util.timer.Timers;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;
import static java.util.Arrays.asList;

public class AntiAFK extends Module {
    private final Timer swingTickTimer = Timers.tickTimer();
    private final Timer startWalkTickTimer = Timers.tickTimer();
    private final Timer rotateTimer = Timers.tickTimer();
    private final Timer jumpTimer = Timers.tickTimer();
    private final Timer sneakTimer = Timers.tickTimer();
    private boolean shouldSneak = false;
    private boolean shouldWalk = false;
    private final List<WalkDirection> walkDirections = asList(
            new WalkDirection(1, 0), new WalkDirection(-1, 0),
            new WalkDirection(1, 1), new WalkDirection(-1, -1),
            new WalkDirection(0, -1), new WalkDirection(0, 1),
            new WalkDirection(-1, 1), new WalkDirection(1, -1),
            new WalkDirection(-1, 0), new WalkDirection(1, 0),
            new WalkDirection(1, -1), new WalkDirection(-1, 1),
            new WalkDirection(0, 1), new WalkDirection(0, -1)
    );
    private final Iterator<WalkDirection> walkDirectionIterator = Iterators.cycle(walkDirections);
    private BlockPos currentPathingGoal;
    public static final int MOVEMENT_PRIORITY = 100;

    @Override
    public List<EventConsumer<?>> registerEvents() {
        return List.of(
            of(ClientBotTick.class, this::handleClientTickEvent),
            of(ClientBotTick.Starting.class, this::handleClientBotTickStarting),
            of(ClientBotTick.Stopped.class, this::handleClientBotTickStopped),
            of(ClientDeathEvent.class, this::handleDeathEvent)
        );
    }

    @Override
    public boolean enabledSetting() {
        return CONFIG.client.extra.antiafk.enabled;
    }

    public void handleClientTickEvent(final ClientBotTick event) {
        if (CACHE.getPlayerCache().getThePlayer().isAlive()) {
            if (CONFIG.client.extra.antiafk.actions.swingHand) {
                swingTick();
            }
            if (CONFIG.client.extra.antiafk.actions.rotate) {
                rotateTick();
            }
            if (CONFIG.client.extra.antiafk.actions.jump) {
                jumpTick();
            }
            if (CONFIG.client.extra.antiafk.actions.sneak) {
                sneakTick();
            }
            if (CONFIG.client.extra.antiafk.actions.walk) {
                walkTick();
            }
        }
    }

    private void sneakTick() {
        if (sneakTimer.tick(100L)) {
            shouldSneak = !shouldSneak;
        }
        if (shouldSneak) {
            INPUTS.submit(InputRequest.builder()
                .owner(this)
                .input(Input.builder()
                    .sneaking(true)
                    .build())
                .priority(MOVEMENT_PRIORITY - 1)
                .build());
        }
    }

    public void handleDeathEvent(final ClientDeathEvent event) {
        synchronized (this) {
            reset();
        }
    }

    public void handleClientBotTickStarting(final ClientBotTick.Starting event) {
        reset();
    }

    public void handleClientBotTickStopped(final ClientBotTick.Stopped event) {
        reset();
    }

    private synchronized void reset() {
        swingTickTimer.reset();
        startWalkTickTimer.reset();
        rotateTimer.reset();
        shouldSneak = false;
        shouldWalk = false;
        currentPathingGoal = null;
        jumpTimer.reset();
    }

    private void rotateTick() {
        if (rotateTimer.tick(CONFIG.client.extra.antiafk.actions.rotateDelayTicks)) {
            INPUTS.submit(InputRequest.builder()
                .owner(this)
                .yaw(-180 + (360 * ThreadLocalRandom.current().nextFloat()))
                .pitch(-90 + (180 * ThreadLocalRandom.current().nextFloat()))
                .priority(MOVEMENT_PRIORITY - 1)
                .build());
        }
    }

    private void jumpTick() {
        if (jumpTimer.tick(CONFIG.client.extra.antiafk.actions.jumpDelayTicks)) {
            if (CONFIG.client.extra.antiafk.actions.jumpOnlyInWater && !BOT.isTouchingWater()) return;
            INPUTS.submit(InputRequest.builder()
                .owner(this)
                .input(Input.builder()
                    .jumping(true)
                    .build())
                .priority(MOVEMENT_PRIORITY + 1)
                .build());
        }
    }

    public synchronized void handlePlayerPosRotate() {
        this.shouldWalk = false;
    }

    private void walkTick() {
        if (startWalkTickTimer.tick(CONFIG.client.extra.antiafk.actions.walkDelayTicks)) {
            shouldWalk = true;
            final WalkDirection directions = walkDirectionIterator.next();
            var xGoal = World.getCurrentPlayerX() + CONFIG.client.extra.antiafk.actions.walkDistance * directions.from;
            var zGoal = World.getCurrentPlayerZ() + CONFIG.client.extra.antiafk.actions.walkDistance * directions.to;
            currentPathingGoal = new BlockPos(MathHelper.floorI(xGoal), MathHelper.floorI(World.getCurrentPlayerY()), MathHelper.floorI(zGoal));
        }
        if (shouldWalk) {
            if (reachedPathingGoal()) {
                shouldWalk = false;
            } else {
                var shouldSneak = !BOT.isTouchingWater()
                    && (CONFIG.client.extra.antiafk.actions.safeWalk || CONFIG.client.extra.antiafk.actions.sneak);
                INPUTS.submit(InputRequest.builder()
                    .owner(this)
                    .input(Input.builder()
                        .pressingForward(true)
                        .sneaking(shouldSneak)
                        .build())
                    .yaw(RotationHelper.yawToXZ(currentPathingGoal.x() + 0.5, currentPathingGoal.z() + 0.5))
                    .priority(MOVEMENT_PRIORITY)
                    .build());
            }
        }
    }

    private boolean reachedPathingGoal() {
        final int px = MathHelper.floorI(World.getCurrentPlayerX());
        final int pz = MathHelper.floorI(World.getCurrentPlayerZ());
        return px == currentPathingGoal.x() && pz == currentPathingGoal.z();
    }

    private void swingTick() {
        if (swingTickTimer.tick(CONFIG.client.extra.antiafk.actions.swingDelayTicks)) {
            // todo: add a way to request a swing without requiring movement to be stopped
            //  would need some way to mark this input as partial, and logic for combining inputs
            INPUTS.submit(InputRequest.builder()
                .owner(this)
                .input(Input.builder()
                    .leftClick(true)
                    .clickTarget(ClickTarget.None.INSTANCE)
                    .sneaking((CONFIG.client.extra.antiafk.actions.walk && CONFIG.client.extra.antiafk.actions.safeWalk) || CONFIG.client.extra.antiafk.actions.sneak)
                    .build())
                .priority(MOVEMENT_PRIORITY * 10)
                .build());
        }
    }

    record WalkDirection(int from, int to) { }
}
