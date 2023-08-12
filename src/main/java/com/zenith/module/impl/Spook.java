package com.zenith.module.impl;

import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerRotationPacket;
import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.event.Subscription;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.event.proxy.NewPlayerInVisualRangeEvent;
import com.zenith.module.Module;
import com.zenith.util.TickTimer;

import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static com.zenith.Shared.*;
import static com.zenith.event.SimpleEventBus.pair;
import static java.util.Objects.isNull;

public class Spook extends Module {
    public final AtomicBoolean hasTarget;
    private final TickTimer stareTimer;
    private final Stack<EntityPlayer> focusStack;

    public Spook() {
        super();
        this.stareTimer = new TickTimer();
        this.hasTarget = new AtomicBoolean(false);
        this.focusStack = new Stack<>();
    }

    @Override
    public Subscription subscribeEvents() {
        return EVENT_BUS.subscribe(
            pair(ClientTickEvent.class, this::handleClientTickEvent),
            pair(NewPlayerInVisualRangeEvent.class, this::handleNewPlayerInVisualRangeEvent));
    }

    @Override
    public Supplier<Boolean> shouldBeEnabled() {
        return () -> CONFIG.client.extra.spook.enabled;
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
                && MODULE_MANAGER.getModule(KillAura.class).map(ka -> !ka.isActive()).orElse(true)) {
            stareTick();
        } else {
            hasTarget.lazySet(false);
        }
    }


    public void handleNewPlayerInVisualRangeEvent(NewPlayerInVisualRangeEvent event) {
        synchronized (this.focusStack) {
            this.focusStack.push(event.playerEntity);
        }
    }

    public static float getPitch(Entity entity) {
        PlayerCache player = CACHE.getPlayerCache();
        double y = entity.getY() + 1.6; // eye height
        double diffX = entity.getX() - player.getX();
        double diffY = y - (player.getY() + 1.6);
        double diffZ = entity.getZ() - player.getZ();

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        return player.getPitch() + wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)) - player.getPitch());
    }

    public static float getYaw(Entity entity) {
        PlayerCache player = CACHE.getPlayerCache();
        return player.getYaw() + wrapDegrees((float) Math.toDegrees(Math.atan2(entity.getZ() - player.getZ(), entity.getX() - player.getX())) - 90f - player.getYaw());
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
            sendClientPacketAsync(new ClientPlayerRotationPacket(
                    true,
                    getYaw(nearestPlayer.get()),
                    getPitch(nearestPlayer.get())
            ));
        } else {
            this.hasTarget.set(false);
        }
    }

    private void handleVisualRangeTargetTick() {
        synchronized (focusStack) {
            if (!this.focusStack.isEmpty()) {
                final EntityPlayer target = this.focusStack.peek();
                this.hasTarget.set(true);
                sendClientPacketAsync(new ClientPlayerRotationPacket(
                        true,
                        getYaw(target),
                        getPitch(target)
                ));
            } else {
                this.hasTarget.set(false);
            }
        }
    }

    public static float wrapDegrees(float degrees) {
        float f = degrees % 360.0F;
        if (f >= 180.0F) {
            f -= 360.0F;
        }

        if (f < -180.0F) {
            f += 360.0F;
        }

        return f;
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
