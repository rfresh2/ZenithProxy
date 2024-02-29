package com.zenith.module.impl;

import com.zenith.Proxy;
import com.zenith.event.module.ClientOnlineTickEvent;
import com.zenith.event.module.ClientSwingEvent;
import com.zenith.event.module.EntityFishHookSpawnEvent;
import com.zenith.event.proxy.ProxyClientConnectedEvent;
import com.zenith.module.Module;
import com.zenith.util.math.MathHelper;

import java.time.Duration;
import java.time.Instant;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;

public class AntiKick extends Module {
    private double lastPosX = 0;
    private double lastPosZ = 0;
    private Instant lastPosTime = Instant.now();
    private Instant lastFishingTime = Instant.now();
    private Instant lastSwingTime = Instant.now();

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(
            this,
            of(ProxyClientConnectedEvent.class, this::onProxyClientConnectedEvent),
            of(ClientOnlineTickEvent.class, this::onClientOnlineTick),
            of(EntityFishHookSpawnEvent.class, this::onEntityFishHookSpawnEvent),
            of(ClientSwingEvent.class, this::onClientSwingEvent)
        );
    }

    @Override
    public boolean shouldBeEnabled() {
        return CONFIG.client.extra.antiKick.enabled;
    }

    @Override
    public void onEnable() {
        reset();
    }

    private synchronized void reset() {
        this.lastPosX = CACHE.getPlayerCache().getX();
        this.lastPosZ = CACHE.getPlayerCache().getZ();
        this.lastPosTime = this.lastFishingTime = this.lastSwingTime = Instant.now();
    }

    public void onProxyClientConnectedEvent(final ProxyClientConnectedEvent event) {
        reset();
    }

    public void onClientOnlineTick(final ClientOnlineTickEvent event) {
        if (!Proxy.getInstance().hasActivePlayer()) return;

        final var fishBypass = bypassedKickWithFishing();
        final var swingBypass = bypassedKickWithSwing();
        final var walkBypass = bypassedKickWithWalk();
        if (fishBypass || swingBypass || walkBypass) return;
        final var currentPlayer = Proxy.getInstance().getCurrentPlayer().get();
        if (currentPlayer == null) return;
        currentPlayer.disconnect("[AntiKick] Kicked for inactivity");
    }

    public void onEntityFishHookSpawnEvent(final EntityFishHookSpawnEvent event) {
        if (event.getOwnerEntityId() == CACHE.getPlayerCache().getEntityId())
            lastFishingTime = Instant.now();
    }

    public void onClientSwingEvent(final ClientSwingEvent event) {
        lastSwingTime = Instant.now();
    }

    private boolean bypassedKickWithFishing() {
        return Instant.now()
            .minus(Duration.ofMinutes(CONFIG.client.extra.antiKick.playerInactivityKickMins))
            .isBefore(lastFishingTime);
    }

    private boolean bypassedKickWithSwing() {
        return Instant.now()
            .minus(Duration.ofMinutes(CONFIG.client.extra.antiKick.playerInactivityKickMins))
            .isBefore(lastSwingTime);
    }

    private boolean bypassedKickWithWalk() {
        final var distance = MathHelper.manhattanDistance2d(lastPosX, lastPosZ, CACHE.getPlayerCache().getX(), CACHE.getPlayerCache().getZ());
        if (distance >= CONFIG.client.extra.antiKick.minWalkDistance) {
            lastPosX = CACHE.getPlayerCache().getX();
            lastPosZ = CACHE.getPlayerCache().getZ();
            lastPosTime = Instant.now();
            return true;
        } else {
            return Instant.now()
                .minus(Duration.ofMinutes(CONFIG.client.extra.antiKick.playerInactivityKickMins))
                .isBefore(lastPosTime);
        }
    }
}
