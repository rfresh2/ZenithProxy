package com.zenith.module;

import com.collarmc.pounce.Subscribe;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerSwingArmPacket;
import com.zenith.Proxy;
import com.zenith.event.ClientTickEvent;
import com.zenith.util.TickTimer;

import static com.zenith.util.Constants.*;
import static java.util.Objects.isNull;

public class AntiAFK extends Module {
    private final TickTimer swingTickTimer = new TickTimer();
    private final TickTimer startWalkTickTimer = new TickTimer();
    private final TickTimer walkTickTimer = new TickTimer();
    private boolean shouldWalk = false;
    // toggle this between 1 and -1
    private double xDirectionMultiplier = 1.0;

    public AntiAFK(Proxy proxy) {
        super(proxy);
    }

    @Subscribe
    public void handleClientTickEvent(final ClientTickEvent event) {
        if (CONFIG.client.extra.antiafk.enabled && isNull(this.proxy.getCurrentPlayer().get())) {
            if (CONFIG.client.extra.antiafk.actions.swingHand) {
                swingTick();
            }
            if (CONFIG.client.extra.antiafk.actions.walk) {
                walkTick();
            }
        }
    }

    private void walkTick() {
        if (startWalkTickTimer.tick(1200L, true)) {
            shouldWalk = !shouldWalk;
            walkTickTimer.reset();
        }
        if (shouldWalk) {
            if (walkTickTimer.tick(100L, true)) {
                shouldWalk = false;
                xDirectionMultiplier *= -1.0;
            } else {
                // calculate a walk, to keep things simple let's just walk +x and -x
                double newX = CACHE.getPlayerCache().getX() + (0.2 * xDirectionMultiplier);
                CLIENT_LOG.debug("Walking to new X: " + newX);
                this.proxy.getClient().getSession().send(
                        new ClientPlayerPositionPacket(
                                true,
                                newX,
                                CACHE.getPlayerCache().getY(),
                                CACHE.getPlayerCache().getZ()));
                CACHE.getPlayerCache().setX(newX);
            }
        }
    }

    private void swingTick() {
        if (swingTickTimer.tick(3000L, true)) {
            this.proxy.getClient().getSession().send(new ClientPlayerSwingArmPacket(Hand.MAIN_HAND));
        }
    }
}
