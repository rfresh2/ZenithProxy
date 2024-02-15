package com.zenith.module.impl;

import com.zenith.Proxy;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.event.proxy.NewPlayerInVisualRangeEvent;
import com.zenith.feature.pathing.Pathing;
import com.zenith.module.Module;
import com.zenith.util.TickTimer;
import org.cloudburstmc.math.vector.Vector2f;

import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;
import static java.util.Objects.isNull;

public class Spook extends Module {
    public final AtomicBoolean hasTarget;
    private final TickTimer stareTimer;
    private final Stack<EntityPlayer> focusStack;
    private static final int MOVEMENT_PRIORITY = 10;

    public Spook() {
        super();
        this.stareTimer = new TickTimer();
        this.hasTarget = new AtomicBoolean(false);
        this.focusStack = new Stack<>();
    }

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(this,
                            of(ClientTickEvent.class, this::handleClientTickEvent),
                            of(NewPlayerInVisualRangeEvent.class, this::handleNewPlayerInVisualRangeEvent));
    }

    @Override
    public boolean shouldBeEnabled() {
        return CONFIG.client.extra.spook.enabled;
    }

    @Override
    public void onEnable() {
        CACHE.getEntityCache().getEntities().values().stream()
                .filter(entity -> entity instanceof EntityPlayer && !entity.equals(CACHE.getPlayerCache().getThePlayer()))
                .map(entity -> (EntityPlayer) entity)
                .forEach(this.focusStack::push);
    }

    public void handleClientTickEvent(final ClientTickEvent event) {
        synchronized (focusStack) { // handling this regardless of mode so we don't fill stack indefinitely
            if (!this.focusStack.isEmpty()) {
                this.focusStack.removeIf(e -> CACHE.getEntityCache().getEntities().values().stream()
                        .noneMatch(entity -> Objects.equals(e, entity)));
            }
        }
        if (isNull(Proxy.getInstance().getCurrentPlayer().get())
                && !Proxy.getInstance().isInQueue()
                && !MODULE_MANAGER.get(KillAura.class).isActive()) {
            stareTick();
        } else {
            hasTarget.lazySet(false);
        }
    }


    public void handleNewPlayerInVisualRangeEvent(NewPlayerInVisualRangeEvent event) {
        synchronized (this.focusStack) {
            this.focusStack.push(event.playerEntity());
        }
    }

    private void stareTick() {
        if (stareTimer.tick(CONFIG.client.extra.spook.tickDelay, true)) {
            switch (CONFIG.client.extra.spook.spookTargetingMode) {
                case NEAREST:
                    handleNearestTargetTick();
                    break;
                case VISUAL_RANGE:
                    handleVisualRangeTargetTick();
                    break;
            }
        }
    }

    private void handleNearestTargetTick() {
        final Optional<EntityPlayer> nearestPlayer = getNearestPlayer();
        if (nearestPlayer.isPresent()) {
            this.hasTarget.set(true);
            Vector2f rotationTo = Pathing.rotationTo(nearestPlayer.get().getX(),
                                                     nearestPlayer.get().getY()+1.6,
                                                     nearestPlayer.get().getZ());
            PATHING.rotate(rotationTo.getX(), rotationTo.getY(), MOVEMENT_PRIORITY);
        } else {
            this.hasTarget.set(false);
        }
    }

    private void handleVisualRangeTargetTick() {
        synchronized (focusStack) {
            if (!this.focusStack.isEmpty()) {
                final EntityPlayer target = this.focusStack.peek();
                this.hasTarget.set(true);
                Vector2f rotationTo = Pathing.rotationTo(target.getX(),
                                                      target.getY() +1.6,
                                                      target.getZ());
                PATHING.rotate(rotationTo.getX(), rotationTo.getY(), MOVEMENT_PRIORITY);
            } else {
                this.hasTarget.set(false);
            }
        }
    }

    private Optional<EntityPlayer> getNearestPlayer() {
        return CACHE.getEntityCache().getEntities().values().stream()
                .filter(entity -> entity instanceof EntityPlayer)
                .map(entity -> (EntityPlayer) entity)
                .filter(e -> e != CACHE.getPlayerCache().getThePlayer())
                .min((e1, e2) -> getDistanceToPlayer(e1) - getDistanceToPlayer(e2));
    }

    private double getDistance(final EntityPlayer p1, final EntityPlayer p2) {
        return Math.sqrt(Math.pow(p2.getX() - p1.getX(), 2) + Math.pow(p2.getY() - p1.getY(), 2) + Math.pow(p2.getZ() - p1.getZ(), 2));
    }

    private int getDistanceToPlayer(final EntityPlayer e) {
        return (int) getDistance(e, CACHE.getPlayerCache().getThePlayer());
    }
}
