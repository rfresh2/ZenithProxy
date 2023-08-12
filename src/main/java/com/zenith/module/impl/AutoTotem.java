package com.zenith.module.impl;

import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.window.ClickItemParam;
import com.github.steveice10.mc.protocol.data.game.window.WindowAction;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientWindowActionPacket;
import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.module.Module;

import java.time.Duration;
import java.time.Instant;

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
        EVENT_BUS.subscribe(ClientTickEvent.class, this::handleClientTick);
    }

    public void handleClientTick(final ClientTickEvent event) {
        if (CONFIG.client.extra.autoTotem.enabled
                && CACHE.getPlayerCache().getThePlayer().getHealth() > 0
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
        return CACHE.getPlayerCache().getThePlayer().getHealth() <= CONFIG.client.extra.autoTotem.healthThreshold;
    }

    private boolean isTotemEquipped() {
        final ItemStack offhandStack = CACHE.getPlayerCache().getThePlayer().getEquipment().get(EquipmentSlot.OFF_HAND);
        return nonNull(offhandStack) && offhandStack.getId() == 449;
    }

    private void swapToTotem() {
        final ItemStack[] inventory = CACHE.getPlayerCache().getInventory();
        ItemStack offhand = inventory[45];
        for (int i = 44; i >= 9; i--) {
            final ItemStack stack = inventory[i];
            if (nonNull(stack) && stack.getId() == 449) {
                if (nonNull(offhand) && nonNull(CACHE.getPlayerCache().getThePlayer().getEquipment().get(EquipmentSlot.OFF_HAND))) {
                    sendClientPacketAsync(new ClientWindowActionPacket(0, actionId++, i, stack, WindowAction.CLICK_ITEM, ClickItemParam.LEFT_CLICK));
                    sendClientPacketAsync(new ClientWindowActionPacket(0, actionId++, 45, offhand, WindowAction.CLICK_ITEM, ClickItemParam.LEFT_CLICK));
                    sendClientPacketAsync(new ClientWindowActionPacket(0, actionId++, i, new ItemStack(0, 0), WindowAction.CLICK_ITEM, ClickItemParam.LEFT_CLICK));
                } else {
                    sendClientPacketAsync(new ClientWindowActionPacket(0, actionId++, i, stack, WindowAction.CLICK_ITEM, ClickItemParam.LEFT_CLICK));
                    sendClientPacketAsync(new ClientWindowActionPacket(0, actionId++, 45, new ItemStack(0, 0), WindowAction.CLICK_ITEM, ClickItemParam.LEFT_CLICK));
                }
                CLIENT_LOG.info("Swapping to totem");
                delay = 5;
                swapping = true;
                return;
            }
        }
    }
}
