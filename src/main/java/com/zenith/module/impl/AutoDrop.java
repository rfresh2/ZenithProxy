package com.zenith.module.impl;

import com.github.rfresh2.EventConsumer;
import com.zenith.event.client.ClientBotTick;
import com.zenith.feature.inventory.InventoryActionRequest;
import com.zenith.feature.inventory.actions.DropItem;
import com.zenith.feature.inventory.util.InventoryUtil;
import com.zenith.mc.item.ItemRegistry;
import com.zenith.module.api.Module;
import com.zenith.util.timer.Timer;
import com.zenith.util.timer.Timers;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.DropItemAction;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.INVENTORY;

public class AutoDrop extends Module {
    public static final int MOVEMENT_PRIORITY = 20;
    private final Timer dropTimer = Timers.tickTimer();

    @Override
    public boolean enabledSetting() {
        return CONFIG.client.extra.autoDrop.enabled;
    }

    public List<EventConsumer<?>> registerEvents() {
        return List.of(
            of(ClientBotTick.class, this::onTick)
        );
    }

    public void onTick(ClientBotTick event) {
        if (!dropTimer.tick(CONFIG.client.extra.autoDrop.delayTicks)) return;
        int slotId = InventoryUtil.searchPlayerInventory(this::dropItemPredicate);
        if (slotId == -1) return;
        var request = InventoryActionRequest.builder()
            .owner(this)
            .actions(new DropItem(slotId, DropItemAction.DROP_SELECTED_STACK))
            .priority(MOVEMENT_PRIORITY)
            .build();
        INVENTORY.submit(request);
    }

    private boolean dropItemPredicate(@Nullable ItemStack item) {
        if (item == null) return false;
        var itemData = ItemRegistry.REGISTRY.get(item.getId());
        if (itemData == null) return false;
        return switch (CONFIG.client.extra.autoDrop.mode) {
            case ALL -> true;
            case WHITELIST -> CONFIG.client.extra.autoDrop.items.contains(itemData.name());
            case BLACKLIST -> !CONFIG.client.extra.autoDrop.items.contains(itemData.name());
        };
    }
}
