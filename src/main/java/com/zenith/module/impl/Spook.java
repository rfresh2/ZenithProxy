package com.zenith.module.impl;

import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.event.module.ClientBotTick;
import com.zenith.event.proxy.DisconnectEvent;
import com.zenith.event.proxy.NewPlayerInVisualRangeEvent;
import com.zenith.event.proxy.PlayerLeftVisualRangeEvent;
import com.zenith.feature.world.Pathing;
import com.zenith.module.Module;
import com.zenith.util.Timer;
import com.zenith.util.math.MathHelper;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;

public class Spook extends Module {
    private final Timer searchTimer = Timer.createTickTimer();
    // list (used as a stack) of most recently seen player entity ID's
    private final IntArrayList playerTargetStack = new IntArrayList();
    private int targetEntity = -1;
    private static final int MOVEMENT_PRIORITY = 10; // relatively low
    private static final int SEARCH_DELAY_TICKS = 50;

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(
            this,
            of(ClientBotTick.class, this::handleClientTickEvent),
            of(DisconnectEvent.class, this::handleDisconnectEvent),
            of(NewPlayerInVisualRangeEvent.class, this::handleNewPlayerInVisualRangeEvent),
            of(PlayerLeftVisualRangeEvent.class, this::handlePlayerLeftVisualRangeEvent));
    }

    @Override
    public boolean enabledSetting() {
        return CONFIG.client.extra.spook.enabled;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onEnable() {
        CACHE.getEntityCache().getEntities().values().stream()
                .filter(entity -> entity instanceof EntityPlayer && !entity.equals(CACHE.getPlayerCache().getThePlayer()))
                .map(Entity::getEntityId)
                .forEach(this.playerTargetStack::push);
    }

    @Override
    public void onDisable() {
        this.playerTargetStack.clear();
    }

    private void handleClientTickEvent(final ClientBotTick event) {
        if (searchTimer.tick(SEARCH_DELAY_TICKS)) {
            EXECUTOR.execute(this::searchForTarget);
        }
        rotateToTarget();
    }


    private void handleNewPlayerInVisualRangeEvent(NewPlayerInVisualRangeEvent event) {
        synchronized (this.playerTargetStack) {
            this.playerTargetStack.push(event.playerEntity().getEntityId());
        }
    }

    private void handlePlayerLeftVisualRangeEvent(PlayerLeftVisualRangeEvent event) {
        synchronized (this.playerTargetStack) {
            this.playerTargetStack.rem(event.playerEntity().getEntityId());
        }
    }

    private void handleDisconnectEvent(DisconnectEvent event) {
        synchronized (this.playerTargetStack) {
            this.playerTargetStack.clear();
        }
    }

    private void searchForTarget() {
        synchronized (playerTargetStack) { // handling this regardless of mode so we don't fill stack indefinitely
            if (!this.playerTargetStack.isEmpty()) {
                this.playerTargetStack.removeIf(e -> CACHE.getEntityCache().get(e) == null);
            }
        }
        this.targetEntity = switch (CONFIG.client.extra.spook.spookTargetingMode) {
            case NEAREST -> findNearestTarget();
            case VISUAL_RANGE -> findVisualRangeTarget();
        };
    }

    private int findVisualRangeTarget() {
        synchronized (playerTargetStack) {
            if (!this.playerTargetStack.isEmpty()) {
                return this.playerTargetStack.topInt();
            }
        }
        return -1;
    }

    private int findNearestTarget() {
        return CACHE.getEntityCache().getEntities().values().stream()
            .filter(entity -> entity instanceof EntityPlayer)
            .map(entity -> (EntityPlayer) entity)
            .filter(e -> !e.isSelfPlayer())
            .min((e1, e2) -> (int) (getDistanceToPlayer(e1) - getDistanceToPlayer(e2)))
            .map(Entity::getEntityId)
            .orElse(-1);
    }

    private void rotateToTarget() {
        if (targetEntity != -1) {
            var entity = CACHE.getEntityCache().get(targetEntity);
            if (entity == null) {
                targetEntity = -1;
                return;
            }
            var rotation = Pathing.rotationTo(entity.getX(), entity.getY() + 1.6, entity.getZ());
            PATHING.rotate(rotation.getX(), rotation.getY(), MOVEMENT_PRIORITY);
        }
    }

    private double getDistanceToPlayer(final EntityPlayer e) {
        var player = CACHE.getPlayerCache().getThePlayer();
        return MathHelper.manhattanDistance3d(e.getX(), e.getY(), e.getZ(), player.getX(), player.getY(), player.getZ());
    }
}
