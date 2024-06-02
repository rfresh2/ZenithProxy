package com.zenith.module.impl;

import com.google.common.collect.Iterators;
import com.zenith.event.module.ClientBotTick;
import com.zenith.event.proxy.DeathEvent;
import com.zenith.feature.world.Input;
import com.zenith.feature.world.Pathing;
import com.zenith.mc.block.BlockPos;
import com.zenith.module.Module;
import com.zenith.util.Timer;
import com.zenith.util.math.MathHelper;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket;

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
        EVENT_BUS.subscribe(
            this,
            of(ClientBotTick.class, this::handleClientTickEvent),
            of(ClientBotTick.Starting.class, this::handleClientBotTickStarting),
            of(ClientBotTick.Stopped.class, this::handleClientBotTickStopped),
            of(DeathEvent.class, this::handleDeathEvent)
        );
    }

    @Override
    public boolean shouldBeEnabled() {
        return CONFIG.client.extra.antiafk.enabled;
    }

    public void handleClientTickEvent(final ClientBotTick event) {
        if (CACHE.getPlayerCache().getThePlayer().isAlive()
                && !MODULE.get(KillAura.class).isActive()) {
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
            if (CONFIG.client.extra.antiafk.actions.jumpOnlyInWater && !MODULE.get(PlayerSimulation.class).isTouchingWater()) return;
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
            var xGoal = Pathing.getCurrentPlayerX() + CONFIG.client.extra.antiafk.actions.walkDistance * directions.from;
            var zGoal = Pathing.getCurrentPlayerZ() + CONFIG.client.extra.antiafk.actions.walkDistance * directions.to;
            currentPathingGoal = new BlockPos(MathHelper.floorI(xGoal), MathHelper.floorI(Pathing.getCurrentPlayerY()), MathHelper.floorI(zGoal));
        }
        if (shouldWalk) {
            if (reachedPathingGoal()) {
                shouldWalk = false;
            } else {
                if (!MODULE.get(PlayerSimulation.class).isTouchingWater() && (CONFIG.client.extra.antiafk.actions.safeWalk || CONFIG.client.extra.antiafk.actions.sneak))
                    PATHING.moveRotSneakTowardsBlockPos(currentPathingGoal.getX(),
                                                        currentPathingGoal.getZ(),
                                                        MOVEMENT_PRIORITY);
                else
                    PATHING.moveRotTowardsBlockPos(currentPathingGoal.getX(),
                                                    currentPathingGoal.getZ(),
                                                    MOVEMENT_PRIORITY);
            }
        }
    }

    private boolean reachedPathingGoal() {
        final int px = MathHelper.floorI(Pathing.getCurrentPlayerX());
        final int pz = MathHelper.floorI(Pathing.getCurrentPlayerZ());
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
