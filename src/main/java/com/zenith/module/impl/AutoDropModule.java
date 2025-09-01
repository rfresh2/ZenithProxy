package com.zenith.module.impl;

import com.github.rfresh2.EventConsumer;
import com.zenith.cache.data.inventory.Container;
import com.zenith.event.client.ClientBotTick;
import com.zenith.feature.inventory.InventoryActionRequest;
import com.zenith.feature.inventory.actions.DropItem;
import com.zenith.feature.inventory.actions.InventoryAction;
import com.zenith.mc.item.ItemRegistry;
import com.zenith.module.api.Module;
import com.zenith.util.RequestFuture;
import com.zenith.util.config.Config;
import com.zenith.util.timer.Timer;
import com.zenith.util.timer.Timers;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.DropItemAction;

import java.util.ArrayList;
import java.util.List;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;

/**
 * @author Zenith, mikumiku7
 */
public class AutoDropModule extends Module {

    public static final Config.Client.AutoDrop AUTO_DROP_CONFIG = CONFIG.client.extra.autoDrop;
    public static final int PRIORITY = 8000;
    private final Timer dropTimer = Timers.tickTimer();
    private RequestFuture inventoryActionFuture = RequestFuture.rejected;

    @Override
    public boolean enabledSetting() {
        return AUTO_DROP_CONFIG.enabled;
    }

    public List<EventConsumer<?>> registerEvents() {
        return List.of(
                of(ClientBotTick.class, this::onTick),
                of(ClientBotTick.Stopped.class, e -> reset())
        );
    }

    @Override
    public void onDisable() {
        reset();
    }

    private void reset() {
        dropTimer.reset();
        inventoryActionFuture = RequestFuture.rejected;
    }

    private void onTick(ClientBotTick event) {
        if (!AUTO_DROP_CONFIG.enabled) {
            return;
        }

        if (inventoryActionFuture.isCompleted()) {
            inventoryActionFuture = RequestFuture.rejected;
        } else {
            return;
        }

        if (dropTimer.tick(AUTO_DROP_CONFIG.delayBetweenDrops)) {
            dropTimer.reset();
            performAutoDrop();
        }
    }

    private void performAutoDrop() {
        var inventory = CACHE.getPlayerCache().getPlayerInventory();
        List<InventoryAction> actions = new ArrayList<>();

        for (int i = 9; i <= 44; i++) {
            var itemStack = inventory.get(i);
            if (itemStack == Container.EMPTY_STACK) {
                continue;
            }

            if (shouldDropItem(itemStack.getId())) {
                actions.add(new DropItem(0, i, DropItemAction.DROP_SELECTED_STACK));
            }
        }

        if (!actions.isEmpty()) {
            inventoryActionFuture = INVENTORY.submit(InventoryActionRequest.builder()
                    .owner(this)
                    .actions(actions)
                    .priority(PRIORITY)
                    .build());
        }
    }

    private boolean shouldDropItem(int itemId) {
        var itemData = ItemRegistry.REGISTRY.get(itemId);
        if (itemData == null) {
            return false;
        }

        String itemName = itemData.name();
        boolean isInList = AUTO_DROP_CONFIG.items.contains(itemName);

        return AUTO_DROP_CONFIG.whitelistMode ? isInList : !isInList;
    }
}
