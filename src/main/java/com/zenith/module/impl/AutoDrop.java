package com.zenith.module.impl;

import com.github.rfresh2.EventConsumer;
import com.zenith.event.client.ClientBotTick;
import com.zenith.feature.inventory.InventoryActionRequest;
import com.zenith.feature.inventory.actions.DropItem;
import com.zenith.feature.inventory.util.InventoryUtil;
import com.zenith.feature.player.InputRequest;
import com.zenith.mc.item.ItemRegistry;
import com.zenith.module.api.Module;
import com.zenith.util.math.MathHelper;
import com.zenith.util.timer.Timer;
import com.zenith.util.timer.Timers;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.DropItemAction;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;

public class AutoDrop extends Module {
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

    public int getPriority() {
        return Objects.requireNonNullElse(CONFIG.client.extra.autoDrop.priority, 3000);
    }

    public void onTick(ClientBotTick event) {
        if (!dropTimer.tick(CONFIG.client.extra.autoDrop.delayTicks)) return;
        int slotId = InventoryUtil.searchPlayerInventory(this::dropItemPredicate);
        if (slotId == -1) return;
        if (CONFIG.client.extra.autoDrop.requiresRotation) {
            var inputRequest = InputRequest.builder()
                .owner(this)
                .yaw(CONFIG.client.extra.autoDrop.yaw)
                .pitch(CONFIG.client.extra.autoDrop.pitch)
                .priority(getPriority())
                .build();
            INPUTS.submit(inputRequest);
            if (!MathHelper.isYawInRange(CONFIG.client.extra.autoDrop.yaw, CACHE.getPlayerCache().getYaw(), 0.1f)
                || !MathHelper.isPitchInRange(CONFIG.client.extra.autoDrop.pitch, CACHE.getPlayerCache().getPitch(), 0.1f)) {
                dropTimer.skip();
                // await rotation next tick
                return;
            }
        }
        var request = InventoryActionRequest.builder()
            .owner(this)
            .actions(new DropItem(slotId, CONFIG.client.extra.autoDrop.dropStack
                ? DropItemAction.DROP_SELECTED_STACK
                : DropItemAction.DROP_FROM_SELECTED))
            .priority(getPriority())
            .build();
        INVENTORY.submit(request);
        var itemData = ItemRegistry.REGISTRY.get(CACHE.getPlayerCache().getPlayerInventory().get(slotId).getId());
        debug("Requesting drop for item: {} in slot: {}", itemData == null ? "?" : itemData.name(), slotId);
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
