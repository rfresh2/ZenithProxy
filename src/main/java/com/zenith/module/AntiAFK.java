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

import java.util.concurrent.ThreadLocalRandom;

import static com.zenith.util.Constants.CONFIG;
import static java.util.Objects.isNull;

public class AntiAFK extends Module {
    private final TickTimer swingTickTimer = new TickTimer();
    private final TickTimer startWalkTickTimer = new TickTimer();
    private final TickTimer walkTickTimer = new TickTimer();
    private final TickTimer rotateTimer = new TickTimer();
    private final Pathing pathing;
    private boolean shouldWalk = false;
    private final int walkGoalDelta = 9;
    // toggle this between 1 and -1
    private double directionMultiplier = 1.0;
    private BlockPos currentPathingGoal;

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
            if (CONFIG.client.extra.antiafk.actions.walk) {
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

    private void walkTick() {
        // temp lowering this 200 for testing
        if (startWalkTickTimer.tick(200L, true)) {
            shouldWalk = !shouldWalk;
            walkTickTimer.reset();
            if (shouldWalk) {
                directionMultiplier *= -1.0;
                ;
                currentPathingGoal = pathing.getCurrentPlayerPos().addX(walkGoalDelta * directionMultiplier).addZ(walkGoalDelta * directionMultiplier).toBlockPos();
            }
        }
        if (shouldWalk) {
            if (walkTickTimer.tick(100L, true)) {
                shouldWalk = false;
            } else {
                Position nextMovePos = pathing.calculateNextMove(currentPathingGoal);
                this.proxy.getClient().send(nextMovePos.toPlayerPositionPacket());
            }
        }
    }

    private void swingTick() {
        if (swingTickTimer.tick(3000L, true)) {
            this.proxy.getClient().send(new ClientPlayerSwingArmPacket(Hand.MAIN_HAND));
        }
    }
}
