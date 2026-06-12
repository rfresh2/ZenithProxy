package com.zenith.module.impl;

import com.github.rfresh2.EventConsumer;
import com.zenith.Proxy;
import com.zenith.cache.data.inventory.Container;
import com.zenith.event.module.*;
import com.zenith.event.player.PlayerDisconnectedEvent;
import com.zenith.mc.item.ItemRegistry;
import com.zenith.module.api.Module;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;

public class AutoDisconnect extends Module {
    public static final String AUTODISCONNECT_REASON_PREFIX = "[AutoDisconnect] ";

    public AutoDisconnect() {
        super();
    }

    @Override
    public List<EventConsumer<?>> registerEvents() {
        return List.of(
            of(PlayerHealthChangedEvent.class, this::handleLowPlayerHealthEvent),
            of(WeatherChangeEvent.class, this::handleWeatherChangeEvent),
            of(PlayerDisconnectedEvent.class, this::handleProxyClientDisconnectedEvent),
            of(ServerPlayerInVisualRangeEvent.class, this::handleNewPlayerInVisualRangeEvent),
            of(TotemPopEvent.class, this::handleTotemPopEvent),
            of(AutoReconnectDecisionEvent.class, this::handleAutoReconnectDecisionEvent)
        );
    }

    @Override
    public boolean enabledSetting() {
        return CONFIG.client.extra.utility.actions.autoDisconnect.enabled;
    }

    public void handleLowPlayerHealthEvent(final PlayerHealthChangedEvent event) {
        if (!CONFIG.client.extra.utility.actions.autoDisconnect.healthDisconnect) return;
        if (event.newHealth() <= CONFIG.client.extra.utility.actions.autoDisconnect.health
            && playerConnectedCheck()) {
            info("Health: {} < {}",
                 event.newHealth(),
                 CONFIG.client.extra.utility.actions.autoDisconnect.health);
            EVENT_BUS.postAsync(new HealthAutoDisconnectEvent());
            doDisconnect("Health: " + event.newHealth() + " <= " + CONFIG.client.extra.utility.actions.autoDisconnect.health);
        }
    }

    public void handleWeatherChangeEvent(final WeatherChangeEvent event) {
        if (!CONFIG.client.extra.utility.actions.autoDisconnect.thunder) return;
        if (CACHE.getChunkCache().isRaining()
            && CACHE.getChunkCache().getThunderStrength() > 0.0f
            && playerConnectedCheck()) {
            info("Thunder disconnect");
            doDisconnect("Thunder");
        }
    }

    public void handleProxyClientDisconnectedEvent(PlayerDisconnectedEvent event) {
        if (!CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect) return;
        info("Auto Client Disconnect");
        doDisconnect("Auto Client Disconnect");
    }

    public void handleNewPlayerInVisualRangeEvent(ServerPlayerInVisualRangeEvent event) {
        if (!CONFIG.client.extra.utility.actions.autoDisconnect.onUnknownPlayerInVisualRange) return;
        var playerUUID = event.playerEntity().getUuid();
        if (PLAYER_LISTS.getFriendsList().contains(playerUUID)
            || PLAYER_LISTS.getWhitelist().contains(playerUUID)
            || PLAYER_LISTS.getSpectatorWhitelist().contains(playerUUID)
            || !playerConnectedCheck()
        ) return;
        info("Unknown Player: {} [{}]", event.playerEntry().getProfile());
        doDisconnect("Unknown Player: " + event.playerEntry().getProfile().getName());
    }

    private void handleTotemPopEvent(TotemPopEvent event) {
        if (!CONFIG.client.extra.utility.actions.autoDisconnect.onTotemPop) return;
        if (event.entityId() != CACHE.getPlayerCache().getEntityId()) return;
        if (playerConnectedCheck()) {
            info("Totem popped");
            // delay execution to allow inventory to update
            EXECUTOR.schedule(() -> {
                var totemCount = countTotems();
                if (totemCount < CONFIG.client.extra.utility.actions.autoDisconnect.minTotemsRemaining && playerConnectedCheck()) {
                    doDisconnect("Totem Pop - " + totemCount + " remaining");
                }
            }, 1, TimeUnit.SECONDS);
        }
    }

    private boolean playerConnectedCheck() {
        if (Proxy.getInstance().hasActivePlayer()) {
            var whilePlayerConnected = CONFIG.client.extra.utility.actions.autoDisconnect.whilePlayerConnected;
            if (!whilePlayerConnected)
                debug("Not disconnecting because a player is connected and whilePlayerConnected setting is disabled");
            return whilePlayerConnected;
        }
        return true;
    }

    private void doDisconnect(String reason) {
        Proxy.getInstance().disconnect(AUTODISCONNECT_REASON_PREFIX + reason);
    }

    private void handleAutoReconnectDecisionEvent(AutoReconnectDecisionEvent event) {
        if (isAutoDisconnectReason(event.getDisconnectEvent().reason())
            && (CONFIG.client.extra.utility.actions.autoDisconnect.cancelAutoReconnect || Proxy.getInstance().isPrio())
        ) {
            event.setCancelled(true);
        }
    }

    public static boolean isAutoDisconnectReason(String reason) {
        return reason.startsWith(AUTODISCONNECT_REASON_PREFIX);
    }

    private int countTotems() {
        var count = 0;
        for (ItemStack item : CACHE.getPlayerCache().getPlayerInventory()) {
            if (item != Container.EMPTY_STACK && item.getId() == ItemRegistry.TOTEM_OF_UNDYING.id())
                count++;
        }
        return count;
    }
}
