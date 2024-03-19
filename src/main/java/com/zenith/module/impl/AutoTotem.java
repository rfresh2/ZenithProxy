package com.zenith.module.impl;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.zenith.Proxy;
import com.zenith.cache.data.inventory.Container;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.event.module.NoTotemsEvent;
import com.zenith.event.module.PlayerTotemPopAlertEvent;
import com.zenith.event.proxy.TotemPopEvent;

import java.time.Duration;
import java.time.Instant;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;

public class AutoTotem extends AbstractInventoryModule {
    private int delay = 0;
    private static final int MOVEMENT_PRIORITY = 1000;
    private final int totemId = ITEMS_MANAGER.getItemId("totem_of_undying");
    private Instant lastNoTotemsAlert = Instant.EPOCH;
    private static final Duration noTotemsAlertCooldown = Duration.ofMinutes(30);

    public AutoTotem() {
        super(true, -1, MOVEMENT_PRIORITY);
    }

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(
            this,
            of(ClientTickEvent.class, this::handleClientTick),
            of(TotemPopEvent.class, this::onTotemPopEvent)
        );
    }

    @Override
    public boolean shouldBeEnabled() {
        return CONFIG.client.extra.autoTotem.enabled;
    }

    @Override
    public void clientTickStarting() {
        lastNoTotemsAlert = Instant.EPOCH;
    }

    public void handleClientTick(final ClientTickEvent event) {
        if (CACHE.getPlayerCache().getThePlayer().isAlive()
                && playerHealthBelowThreshold()
                && Instant.now().minus(Duration.ofSeconds(2)).isAfter(Proxy.getInstance().getConnectTime())) {
            if (delay > 0) {
                delay--;
                return;
            }
            delay = doInventoryActions();
        }
        if (CONFIG.client.extra.autoTotem.noTotemsAlert
            && lastNoTotemsAlert.plus(noTotemsAlertCooldown).isBefore(Instant.now())) {
            var totemCount = countTotems();
            if (totemCount < 1) {
                lastNoTotemsAlert = Instant.now();
                MODULE_LOG.info("[AutoTotem] No Totems Left");
                EVENT_BUS.postAsync(new NoTotemsEvent());
            }
        }
    }

    private void onTotemPopEvent(TotemPopEvent totemPopEvent) {
        if (totemPopEvent.entityId() == CACHE.getPlayerCache().getEntityId()) {
            var totemCount = countTotems();
            EVENT_BUS.postAsync(new PlayerTotemPopAlertEvent(totemCount));
            MODULE_LOG.info("Player Totem Popped - {} remaining", totemCount);
        }
    }

    private boolean playerHealthBelowThreshold() {
        return CACHE.getPlayerCache().getThePlayer().getHealth() <= CONFIG.client.extra.autoTotem.healthThreshold;
    }

    private int countTotems() {
        var count = 0;
        for (ItemStack item : CACHE.getPlayerCache().getPlayerInventory()) {
            if (item != Container.EMPTY_STACK && item.getId() == totemId)
                count++;
        }
        return count;
    }

    @Override
    public boolean itemPredicate(ItemStack itemStack) {
        return itemStack.getId() == totemId;
    }
}
