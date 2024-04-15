package com.zenith.module.impl;

import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.event.module.ClientBotTick;
import com.zenith.event.proxy.NewPlayerInVisualRangeEvent;
import com.zenith.feature.pathing.Pathing;
import com.zenith.module.Module;
import com.zenith.util.Timer;
import com.zenith.util.math.MathHelper;
import org.cloudburstmc.math.vector.Vector2f;

import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;

public class Spook extends Module {
    public final AtomicBoolean hasTarget = new AtomicBoolean(false);
    private final Timer stareTimer = Timer.newTickTimer();
    private final Stack<EntityPlayer> focusStack = new Stack<>();
    private static final int MOVEMENT_PRIORITY = 10;

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(this,
                            of(ClientBotTick.class, this::handleClientTickEvent),
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

    public void handleClientTickEvent(final ClientBotTick event) {
        synchronized (focusStack) { // handling this regardless of mode so we don't fill stack indefinitely
            if (!this.focusStack.isEmpty()) {
                this.focusStack.removeIf(e -> CACHE.getEntityCache().getEntities().values().stream()
                        .noneMatch(entity -> Objects.equals(e, entity)));
            }
        }
        if (!MODULE.get(KillAura.class).isActive()) {
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
        if (stareTimer.tick(CONFIG.client.extra.spook.tickDelay)) {
            switch (CONFIG.client.extra.spook.spookTargetingMode) {
                case NEAREST -> handleNearestTargetTick();
                case VISUAL_RANGE -> handleVisualRangeTargetTick();
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
                var target = this.focusStack.peek();
                this.hasTarget.set(true);
                Vector2f rotationTo = Pathing.shortestRotationTo(target);
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
                .min((e1, e2) -> (int) (getDistanceToPlayer(e1) - getDistanceToPlayer(e2)));
    }

    private double getDistanceToPlayer(final EntityPlayer e) {
        var player = CACHE.getPlayerCache().getThePlayer();
        return MathHelper.distanceSq3d(e.getX(), e.getY(), e.getZ(), player.getX(), player.getY(), player.getZ());
    }
}
