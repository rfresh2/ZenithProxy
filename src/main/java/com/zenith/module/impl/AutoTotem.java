package com.zenith.module.impl;

import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.inventory.ClickItemAction;
import com.github.steveice10.mc.protocol.data.game.inventory.ContainerActionType;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.event.Subscription;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.module.Module;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

import static com.zenith.Shared.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class AutoTotem extends Module {
    private int actionId = 0; // todo: might need to track this in cache. this will be inaccurate incrementing in many cases
    private boolean swapping = false;
    private int delay = 0;

    public boolean isActive() {
        return CONFIG.client.extra.autoTotem.enabled && swapping;
    }

    public AutoTotem() {
        super();
    }

    @Override
    public Subscription subscribeEvents() {
        return EVENT_BUS.subscribe(ClientTickEvent.class, this::handleClientTick);
    }

    @Override
    public Supplier<Boolean> shouldBeEnabled() {
        return () -> CONFIG.client.extra.autoTotem.enabled;
    }

    public void handleClientTick(final ClientTickEvent event) {
        if (CACHE.getPlayerCache().getThePlayer().getHealth() > 0
                && playerHealthBelowThreshold()
                && isNull(Proxy.getInstance().getCurrentPlayer().get())
                && Instant.now().minus(Duration.ofSeconds(2)).isAfter(Proxy.getInstance().getConnectTime())
                && !Proxy.getInstance().isInQueue()) {
            if (delay > 0) {
                delay--;
                return;
            } else {
                if (swapping) {
                    PlayerCache.sync();
                    delay = 5;
                    swapping = false;
                    return;
                }
            }
            if (!isTotemEquipped()) {
                swapToTotem();
            }
        }
    }

    private boolean playerHealthBelowThreshold() {
        try {
            return CACHE.getPlayerCache().getThePlayer().getHealth() <= CONFIG.client.extra.autoTotem.healthThreshold;
        } catch (final Throwable e) {
            return false;
        }
    }

    private boolean isTotemEquipped() {
        final ItemStack offhandStack = CACHE.getPlayerCache().getThePlayer().getEquipment().get(EquipmentSlot.OFF_HAND);
        return nonNull(offhandStack) && offhandStack.getId() == 1117;
    }

    private void swapToTotem() {
        final ItemStack[] inventory = CACHE.getPlayerCache().getInventory();
        ItemStack offhand = inventory[45];
        for (int i = 44; i >= 9; i--) {
            final ItemStack stack = inventory[i];
            if (nonNull(stack) && stack.getId() == 1117) {
                if (nonNull(offhand) && nonNull(CACHE.getPlayerCache().getThePlayer().getEquipment().get(EquipmentSlot.OFF_HAND))) {
                    sendClientPacketAsync(new ServerboundContainerClickPacket(0, actionId++, i, ContainerActionType.CLICK_ITEM, ClickItemAction.LEFT_CLICK, stack, Int2ObjectMaps.singleton(i, null)));
                    sendClientPacketAsync(new ServerboundContainerClickPacket(0, actionId++, 45, ContainerActionType.CLICK_ITEM, ClickItemAction.LEFT_CLICK, offhand, Int2ObjectMaps.singleton(45, stack)));
                    sendClientPacketAsync(new ServerboundContainerClickPacket(0, actionId++, i, ContainerActionType.CLICK_ITEM, ClickItemAction.LEFT_CLICK, null, Int2ObjectMaps.singleton(i, offhand)));
                } else {
                    sendClientPacketAsync(new ServerboundContainerClickPacket(0, actionId++, i, ContainerActionType.CLICK_ITEM, ClickItemAction.LEFT_CLICK, stack, Int2ObjectMaps.singleton(i, null)));
                    sendClientPacketAsync(new ServerboundContainerClickPacket(0, actionId++, 45, ContainerActionType.CLICK_ITEM, ClickItemAction.LEFT_CLICK, null, Int2ObjectMaps.singleton(45, stack)));
                }
                CLIENT_LOG.info("Swapping to totem");
                delay = 5;
                swapping = true;
                return;
            }
        }
    }
}
