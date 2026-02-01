package com.zenith.module.impl;

import com.github.rfresh2.EventConsumer;
import com.zenith.Proxy;
import com.zenith.cache.data.inventory.Container;
import com.zenith.event.client.ClientBotTick;
import com.zenith.event.client.ClientTickEvent;
import com.zenith.event.module.NoTotemsEvent;
import com.zenith.event.module.PlayerTotemPopAlertEvent;
import com.zenith.event.module.TotemPopEvent;
import com.zenith.feature.inventory.actions.CloseContainer;
import com.zenith.feature.inventory.actions.MoveToHotbarSlot;
import com.zenith.mc.item.ItemRegistry;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.MoveToHotbarAction;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;
import static java.util.Objects.nonNull;

public class AutoTotem extends AbstractInventoryModule {
    private int delay = 0;
    private Instant lastNoTotemsAlert = Instant.EPOCH;
    private static final Duration noTotemsAlertCooldown = Duration.ofMinutes(30);

    public AutoTotem() {
        super(HandRestriction.OFF_HAND, -1);
    }

    @Override
    public List<EventConsumer<?>> registerEvents() {
        return List.of(
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

    @Override
    public int getPriority() {
        return Objects.requireNonNullElse(CONFIG.client.extra.autoTotem.priority, 13000);
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
                CLIENT_LOG.debug("[{}] Swapping totem to offhand {}", getClass().getSimpleName(), actionSlot.getId());
                if (CACHE.getPlayerCache().getInventoryCache().getOpenContainerId() != 0) {
                    var closePacket = new CloseContainer().packet();
                    if (closePacket != null) sendClientPacketAsync(closePacket);
                }
                var moveToHotbarPacket = new MoveToHotbarSlot(i, actionSlot).packet();
                if (moveToHotbarPacket != null) sendClientPacketAsync(moveToHotbarPacket);
                if (CONFIG.client.inventory.ncpStrict) {
                    var closePacket = new CloseContainer(0).packet();
                    if (closePacket != null) sendClientPacketAsync(closePacket);
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
        if (CACHE.getPlayerCache().getThePlayer().isAlive() && playerHealthBelowThreshold()) {
            // todo: submit a no action inv request if we are holding and think we could pop next tick?
            //  we are ok if the other modules don't mess with offhand
            //  and don't want to block main hand actions unnecessarily
            delay = doInventoryActions();
        }
        if (CONFIG.client.extra.autoTotem.noTotemsAlert
            && lastNoTotemsAlert.plus(noTotemsAlertCooldown).isBefore(Instant.now())
            && Proxy.getInstance().isOnlineForAtLeastDuration(Duration.ofSeconds(5))) {
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
            // delay execution to allow inventory to update
            EXECUTOR.schedule(() -> {
                var totemCount = countTotems();
                EVENT_BUS.postAsync(new PlayerTotemPopAlertEvent(totemCount));
                info("Player Totem Popped - {} remaining", totemCount);
            }, 1, TimeUnit.SECONDS);
        }
    }

    public boolean playerHealthBelowThreshold() {
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
