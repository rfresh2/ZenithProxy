package com.zenith.module.impl;

import com.github.rfresh2.EventConsumer;
import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.event.client.ClientBotTick;
import com.zenith.event.client.ClientDisconnectEvent;
import com.zenith.event.module.ServerPlayerInVisualRangeEvent;
import com.zenith.event.module.ServerPlayerLeftVisualRangeEvent;
import com.zenith.feature.player.InputRequest;
import com.zenith.feature.player.RotationHelper;
import com.zenith.module.api.Module;
import com.zenith.util.math.MathHelper;
import com.zenith.util.timer.Timer;
import com.zenith.util.timer.Timers;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.List;
import java.util.Objects;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;

public class Spook extends Module {
    private final Timer searchTimer = Timers.tickTimer();
    // list (used as a stack) of most recently seen player entity ID's
    private final IntArrayList playerTargetStack = new IntArrayList();
    private int targetEntity = -1;
    private static final int SEARCH_DELAY_TICKS = 50;

    @Override
    public List<EventConsumer<?>> registerEvents() {
        return List.of(
            of(ClientBotTick.class, this::handleClientTickEvent),
            of(ClientDisconnectEvent.class, this::handleDisconnectEvent),
            of(ServerPlayerInVisualRangeEvent.class, this::handleNewPlayerInVisualRangeEvent),
            of(ServerPlayerLeftVisualRangeEvent.class, this::handlePlayerLeftVisualRangeEvent)
        );
    }

    @Override
    public boolean enabledSetting() {
        return CONFIG.client.extra.spook.enabled;
    }

    public int getPriority() {
        return Objects.requireNonNullElse(CONFIG.client.extra.spook.priority, 1000);
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


    private void handleNewPlayerInVisualRangeEvent(ServerPlayerInVisualRangeEvent event) {
        synchronized (this.playerTargetStack) {
            this.playerTargetStack.push(event.playerEntity().getEntityId());
        }
    }

    private void handlePlayerLeftVisualRangeEvent(ServerPlayerLeftVisualRangeEvent event) {
        synchronized (this.playerTargetStack) {
            this.playerTargetStack.rem(event.playerEntity().getEntityId());
        }
    }

    private void handleDisconnectEvent(ClientDisconnectEvent event) {
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
            var rotation = RotationHelper.rotationTo(entity.getX(), entity.getY() + 1.6, entity.getZ());
            INPUTS.submit(InputRequest.builder()
                .owner(this)
                .yaw(rotation.getX())
                .pitch(rotation.getY())
                .priority(getPriority())
                .build());
        }
    }

    private double getDistanceToPlayer(final EntityPlayer e) {
        var player = CACHE.getPlayerCache().getThePlayer();
        return MathHelper.manhattanDistance3d(e.getX(), e.getY(), e.getZ(), player.getX(), player.getY(), player.getZ());
    }
}
