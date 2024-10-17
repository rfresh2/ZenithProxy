package com.zenith.module.impl;

import com.zenith.Proxy;
import com.zenith.cache.data.inventory.Container;
import com.zenith.event.module.ClientBotTick;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.event.module.NoTotemsEvent;
import com.zenith.event.module.PlayerTotemPopAlertEvent;
import com.zenith.event.proxy.TotemPopEvent;
import com.zenith.feature.items.ContainerClickAction;
import com.zenith.mc.item.ItemRegistry;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerActionType;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.MoveToHotbarAction;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClosePacket;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;
import static java.util.Objects.nonNull;

public class AutoTotem extends AbstractInventoryModule {
    private int delay = 0;
    private static final int MOVEMENT_PRIORITY = 1000;
    private Instant lastNoTotemsAlert = Instant.EPOCH;
    private static final Duration noTotemsAlertCooldown = Duration.ofMinutes(30);

    public AutoTotem() {
        super(true, -1, MOVEMENT_PRIORITY);
    }

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(
            this,
            of(ClientBotTick.class, this::handleClientBotTick),
            of(ClientBotTick.Starting.class, this::handleBotTickStarting),
            of(TotemPopEvent.class, this::onTotemPopEvent),
            of(ClientTickEvent.class, this::handleClientTick)
        );
    }

    @Override
    public boolean enabledSetting() {
        return CONFIG.client.extra.autoTotem.enabled;
    }

    // todo: these are not synced at all to the player's normal tick loop (neither bot nor controlling player)
    //  its possible this will cause desyncs or flag anticheats
    //  grim doesn't care much about inventory actions so it should be fine on 2b2t
    private void handleClientTick(ClientTickEvent event) {
        if (!CONFIG.client.extra.autoTotem.inGame) return;
        if (!Proxy.getInstance().hasActivePlayer()) return;
        if (delay > 0) {
            delay--;
            return;
        }
        if (CACHE.getPlayerCache().getThePlayer().isAlive()
            && playerHealthBelowThreshold()) {
            if (isItemEquipped()) return;
            if (switchToTotemManual()) {
                delay = 1;
            }
        }
    }

    private boolean switchToTotemManual() {
        final List<ItemStack> inventory = CACHE.getPlayerCache().getPlayerInventory();
        for (int i = 44; i >= 9; i--) {
            ItemStack itemStack = inventory.get(i);
            if (nonNull(itemStack) && itemStack.getId() == ItemRegistry.TOTEM_OF_UNDYING.id()) {
                var actionSlot = MoveToHotbarAction.OFF_HAND;
                var action = new ContainerClickAction(i, ContainerActionType.MOVE_TO_HOTBAR_SLOT, actionSlot);
                CLIENT_LOG.debug("[{}] Swapping totem to offhand {}", getClass().getSimpleName(), actionSlot.getId());
                if (CACHE.getPlayerCache().getInventoryCache().getOpenContainerId() != 0)
                    sendClientPacketAsync(new ServerboundContainerClosePacket(CACHE.getPlayerCache().getInventoryCache().getOpenContainerId()));
                sendClientPacketAsync(action.toPacket());
                if (CONFIG.debug.ncpStrictInventory) {
                    sendClientPacketAsync(new ServerboundContainerClosePacket(0));
                }
                return true;
            }
        }
        return false;
    }

    public void handleBotTickStarting(final ClientBotTick.Starting event) {
        lastNoTotemsAlert = Instant.EPOCH;
    }

    public void handleClientBotTick(final ClientBotTick event) {
        if (delay > 0) {
            delay--;
            return;
        }
        if (CACHE.getPlayerCache().getThePlayer().isAlive()
                && playerHealthBelowThreshold()
                && Proxy.getInstance().getOnlineTimeSeconds() > 2) {
            delay = doInventoryActions();
        }
        if (CONFIG.client.extra.autoTotem.noTotemsAlert
            && lastNoTotemsAlert.plus(noTotemsAlertCooldown).isBefore(Instant.now())) {
            var totemCount = countTotems();
            if (totemCount < 1) {
                lastNoTotemsAlert = Instant.now();
                info("No Totems Left");
                EVENT_BUS.postAsync(new NoTotemsEvent());
            }
        }
    }

    private void onTotemPopEvent(TotemPopEvent totemPopEvent) {
        if (totemPopEvent.entityId() == CACHE.getPlayerCache().getEntityId()) {
            var totemCount = countTotems();
            EVENT_BUS.postAsync(new PlayerTotemPopAlertEvent(totemCount));
            info("Player Totem Popped - {} remaining", totemCount);
        }
    }

    private boolean playerHealthBelowThreshold() {
        return CACHE.getPlayerCache().getThePlayer().getHealth() <= CONFIG.client.extra.autoTotem.healthThreshold;
    }

    private int countTotems() {
        var count = 0;
        for (ItemStack item : CACHE.getPlayerCache().getPlayerInventory()) {
            if (item != Container.EMPTY_STACK && item.getId() == ItemRegistry.TOTEM_OF_UNDYING.id())
                count++;
        }
        return count;
    }

    @Override
    public boolean itemPredicate(ItemStack itemStack) {
        return itemStack.getId() == ItemRegistry.TOTEM_OF_UNDYING.id();
    }
}
