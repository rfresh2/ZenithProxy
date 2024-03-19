package com.zenith.module.impl;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.zenith.Proxy;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.event.proxy.TotemPopEvent;

import java.time.Duration;
import java.time.Instant;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;

public class AutoTotem extends AbstractInventoryModule {
    private int delay = 0;
    private static final int MOVEMENT_PRIORITY = 1000;
    private final int totemId = ITEMS_MANAGER.getItemId("totem_of_undying");

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
    }

    private void onTotemPopEvent(TotemPopEvent totemPopEvent) {
        MODULE_LOG.info("Player Totem Popped");
    }

    private boolean playerHealthBelowThreshold() {
        return CACHE.getPlayerCache().getThePlayer().getHealth() <= CONFIG.client.extra.autoTotem.healthThreshold;
    }

    @Override
    public boolean itemPredicate(ItemStack itemStack) {
        return itemStack.getId() == totemId;
    }
}
