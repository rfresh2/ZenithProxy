package com.zenith.module.impl;

import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerState;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket;
import com.google.common.collect.Iterators;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.event.proxy.DeathEvent;
import com.zenith.feature.pathing.BlockPos;
import com.zenith.feature.pathing.Input;
import com.zenith.feature.pathing.Pathing;
import com.zenith.module.Module;
import com.zenith.util.Timer;
import com.zenith.util.math.MathHelper;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;
import static java.util.Arrays.asList;

public class AntiAFK extends Module {
    private final Timer swingTickTimer = Timer.newTickTimer();
    private final Timer startWalkTickTimer = Timer.newTickTimer();
    private final Timer rotateTimer = Timer.newTickTimer();
    private final Timer jumpTimer = Timer.newTickTimer();
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
    public void subscribeEvents() {
        EVENT_BUS.subscribe(this,
                            of(ClientTickEvent.class, this::handleClientTickEvent),
                            of(DeathEvent.class, this::handleDeathEvent)
        );
    }

    @Override
    public boolean shouldBeEnabled() {
        return CONFIG.client.extra.antiafk.enabled;
    }

    public void handleClientTickEvent(final ClientTickEvent event) {
        if (CACHE.getPlayerCache().getThePlayer().isAlive()
                && !MODULE_MANAGER.get(KillAura.class).isActive()) {
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
        PATHING.move(
                new Input(false, false, false, false, false, true, false),
                MOVEMENT_PRIORITY - 1);
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
    public void clientTickStopped() {
        reset();
    }

    private synchronized void reset() {
        swingTickTimer.reset();
        startWalkTickTimer.reset();
        rotateTimer.reset();
        shouldWalk = false;
        currentPathingGoal = null;
        jumpTimer.reset();
    }

    private void rotateTick() {
        if (rotateTimer.tick(CONFIG.client.extra.antiafk.actions.rotateDelayTicks)) {
            PATHING.rotate(
                -180 + (360 * ThreadLocalRandom.current().nextFloat()),
                -90 + (180 * ThreadLocalRandom.current().nextFloat()),
                MOVEMENT_PRIORITY - 1
            );
        }
    }

    private void jumpTick() {
        if (jumpTimer.tick(CONFIG.client.extra.antiafk.actions.jumpDelayTicks)) {
            PATHING.jump(MOVEMENT_PRIORITY + 1);
        }
    }

    public synchronized void handlePlayerPosRotate() {
        this.shouldWalk = false;
    }

    private void walkTick() {
        if (startWalkTickTimer.tick(CONFIG.client.extra.antiafk.actions.walkDelayTicks)) {
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
                if (CONFIG.client.extra.antiafk.actions.safeWalk || CONFIG.client.extra.antiafk.actions.sneak)
                    PATHING.moveRotSneakTowardsBlockPos(MathHelper.floorToInt(currentPathingGoal.getX()),
                                                        MathHelper.floorToInt(currentPathingGoal.getZ()),
                                                        MOVEMENT_PRIORITY);
                else
                    PATHING.moveRotTowardsBlockPos(MathHelper.floorToInt(currentPathingGoal.getX()),
                                                    MathHelper.floorToInt(currentPathingGoal.getZ()),
                                                    MOVEMENT_PRIORITY);
            }
        }
    }

    private boolean reachedPathingGoal() {
        final int px = MathHelper.floorToInt(Pathing.getCurrentPlayerPos().getX());
        final int pz = MathHelper.floorToInt(Pathing.getCurrentPlayerPos().getZ());
        return px == currentPathingGoal.getX() && pz == currentPathingGoal.getZ();
    }

    private void swingTick() {
        if (swingTickTimer.tick(CONFIG.client.extra.antiafk.actions.swingDelayTicks)) {
            // todo: move this to PlayerSimulation and assign priority so it doesn't conflict with other modules
            sendClientPacketAsync(new ServerboundSwingPacket(Hand.MAIN_HAND));
        }
    }

    record WalkDirection(int from, int to) { }
}
