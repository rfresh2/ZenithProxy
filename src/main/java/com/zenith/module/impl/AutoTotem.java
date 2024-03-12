package com.zenith.module.impl;

import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.inventory.ContainerActionType;
import com.github.steveice10.mc.protocol.data.game.inventory.MoveToHotbarAction;
import com.zenith.Proxy;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.feature.items.ContainerClickAction;
import com.zenith.module.Module;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.zenith.Shared.*;
import static java.util.Objects.nonNull;

public class AutoTotem extends Module {
    private boolean swapping = false;
    private int delay = 0;
    private static final int MOVEMENT_PRIORITY = 1000;
    private int totemId = ITEMS_MANAGER.getItemId("totem_of_undying");

    public AutoTotem() {
        super();
    }

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(this, ClientTickEvent.class, this::handleClientTick);
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
            } else {
                if (swapping) {
                    delay = 5;
                    swapping = false;
                    return;
                }
            }
            if (!isTotemEquipped()) {
                swapToTotem();
            }
            if (swapping) {
                PATHING.stop(MOVEMENT_PRIORITY);
            }
        }
    }

    private boolean playerHealthBelowThreshold() {
        return CACHE.getPlayerCache().getThePlayer().getHealth() <= CONFIG.client.extra.autoTotem.healthThreshold;
    }

    private boolean isTotemEquipped() {
        final ItemStack offhandStack = CACHE.getPlayerCache().getEquipment(EquipmentSlot.OFF_HAND);
        return nonNull(offhandStack) && offhandStack.getId() == totemId;
    }

    private void swapToTotem() {
        final List<ItemStack> inventory = CACHE.getPlayerCache().getPlayerInventory();
        for (int i = 44; i >= 9; i--) {
            final ItemStack stack = inventory.get(i);
            if (nonNull(stack) && stack.getId() == totemId) {
                PLAYER_INVENTORY_MANAGER.invActionReq(
                    this,
                    new ContainerClickAction(
                        i,
                        ContainerActionType.MOVE_TO_HOTBAR_SLOT,
                        MoveToHotbarAction.OFF_HAND
                    ),
                    Integer.MAX_VALUE
                );
                CLIENT_LOG.info("AutoTotem: Swapping to totem");
                delay = 5;
                swapping = true;
                return;
            }
        }
    }
}
