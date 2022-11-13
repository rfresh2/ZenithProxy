package com.zenith.module;

import com.collarmc.pounce.Subscribe;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerSwingArmPacket;
import com.zenith.Proxy;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.pathing.BlockPos;
import com.zenith.pathing.Pathing;
import com.zenith.pathing.Position;
import com.zenith.util.TickTimer;
import javafx.util.Pair;
import org.apache.commons.collections4.iterators.LoopingListIterator;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static com.zenith.util.Constants.CONFIG;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;

public class AntiAFK extends Module {
    private final TickTimer swingTickTimer = new TickTimer();
    private final TickTimer startWalkTickTimer = new TickTimer();
    private final TickTimer walkTickTimer = new TickTimer();
    private final TickTimer rotateTimer = new TickTimer();
    private final Pathing pathing;
    private boolean shouldWalk = false;
    private final int walkGoalDelta = 9;
    private final List<Pair<Integer, Integer>> walkDirections = asList(
            new Pair<>(1, 0), new Pair<>(-1, 0),
            new Pair<>(1, 1), new Pair<>(-1, -1),
            new Pair<>(1, -1), new Pair<>(-1, 1),
            new Pair<>(0, 1), new Pair<>(0, -1));
    private final LoopingListIterator<Pair<Integer, Integer>> walkDirectionIterator = new LoopingListIterator<>(walkDirections);
    private BlockPos currentPathingGoal;
    // tick time since we started falling
    // can be negative, indicates pathing should wait until it reaches 0 to fall
    private int gravityT = 0;

    public AntiAFK(Proxy proxy, final Pathing pathing) {
        super(proxy);
        this.pathing = pathing;
    }

    @Subscribe
    public void handleClientTickEvent(final ClientTickEvent event) {
        if (CONFIG.client.extra.antiafk.enabled && isNull(this.proxy.getCurrentPlayer().get()) && !proxy.isInQueue()) {
            if (CONFIG.client.extra.antiafk.actions.swingHand) {
                swingTick();
            }
            if (CONFIG.client.extra.antiafk.actions.gravity) {
                gravity();
            }
            if (CONFIG.client.extra.antiafk.actions.walk && (!CONFIG.client.extra.antiafk.actions.gravity || gravityT <= 0)) {
                walkTick();
            }
            if (CONFIG.client.extra.antiafk.actions.rotate && (!CONFIG.client.extra.spook.enabled || !spookHasTarget())) {
                rotateTick();
            }
        }
    }

    private boolean spookHasTarget() {
        return this.proxy.getModules().stream()
                .filter(m -> m instanceof Spook)
                .map(m -> ((Spook) m).hasTarget.get())
                .findFirst()
                .orElse(false);
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
        if (startWalkTickTimer.tick(500L, true)) {
            shouldWalk = !shouldWalk;
            walkTickTimer.reset();
            if (shouldWalk) {
                final Pair<Integer, Integer> directions = walkDirectionIterator.next();
                currentPathingGoal = pathing.getCurrentPlayerPos()
                        .addX(walkGoalDelta * directions.getKey())
                        .addZ(walkGoalDelta * directions.getValue())
                        .toBlockPos();
            }
        }
        if (shouldWalk) {
            if (reachedPathingGoal() || walkTickTimer.tick(100L, true)) {
                shouldWalk = false;
            } else {
                Position nextMovePos = pathing.calculateNextMove(currentPathingGoal);
                if (nextMovePos.equals(pathing.getCurrentPlayerPos())) {
                    shouldWalk = false;
                }
                this.proxy.getClient().send(nextMovePos.toPlayerPositionPacket());
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
