package com.zenith.module.impl;

import com.zenith.Proxy;
import com.zenith.event.module.PlayerHealthChangedEvent;
import com.zenith.event.module.WeatherChangeEvent;
import com.zenith.event.proxy.HealthAutoDisconnectEvent;
import com.zenith.event.proxy.NewPlayerInVisualRangeEvent;
import com.zenith.event.proxy.ProxyClientDisconnectedEvent;
import com.zenith.module.Module;
import com.zenith.network.server.ServerConnection;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;
import static java.util.Objects.nonNull;

public class AutoDisconnect extends Module {

    public AutoDisconnect() {
        super();
    }

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(this,
                            of(PlayerHealthChangedEvent.class, this::handleLowPlayerHealthEvent),
                            of(WeatherChangeEvent.class, this::handleWeatherChangeEvent),
                            of(ProxyClientDisconnectedEvent.class, this::handleProxyClientDisconnectedEvent),
                            of(NewPlayerInVisualRangeEvent.class, this::handleNewPlayerInVisualRangeEvent)
        );
    }

    @Override
    public boolean shouldBeEnabled() {
        return CONFIG.client.extra.utility.actions.autoDisconnect.enabled
            || CONFIG.client.extra.utility.actions.autoDisconnect.thunder
            || CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect;
    }

    public void handleLowPlayerHealthEvent(final PlayerHealthChangedEvent event) {
        if (CONFIG.client.extra.utility.actions.autoDisconnect.enabled) {
            if (event.newHealth() <= CONFIG.client.extra.utility.actions.autoDisconnect.health) {
                if (shouldDisconnect()) {
                    info("Health disconnect: {} < {}",
                         event.newHealth(),
                         CONFIG.client.extra.utility.actions.autoDisconnect.health);
                    EVENT_BUS.postAsync(new HealthAutoDisconnectEvent());
                    Proxy.getInstance().disconnect(AUTO_DISCONNECT);
                }
            }
        }
    }

    public void handleWeatherChangeEvent(final WeatherChangeEvent event) {
        if (CONFIG.client.extra.utility.actions.autoDisconnect.thunder) {
            if (CACHE.getChunkCache().isRaining() && CACHE.getChunkCache().getThunderStrength() > 0.0f) {
                if (shouldDisconnect()) {
                    Proxy.getInstance().disconnect(AUTO_DISCONNECT);
                }
            }
        }
    }

    public void handleProxyClientDisconnectedEvent(ProxyClientDisconnectedEvent event) {
        if (CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect) {
            ServerConnection currentConnection = Proxy.getInstance().getCurrentPlayer().get();
            if (nonNull(currentConnection) && currentConnection.getProfileCache().getProfile().equals(event.clientGameProfile())) {
                info("Auto Client Disconnect");
                Proxy.getInstance().disconnect();
            }
        }
    }

    public void handleNewPlayerInVisualRangeEvent(NewPlayerInVisualRangeEvent event) {
        if (CONFIG.client.extra.utility.actions.autoDisconnect.onUnknownPlayerInVisualRange) {
            var playerUUID = event.playerEntity().getUuid();
            if (PLAYER_LISTS.getFriendsList().contains(playerUUID)
                || PLAYER_LISTS.getWhitelist().contains(playerUUID)
                || PLAYER_LISTS.getSpectatorWhitelist().contains(playerUUID)
            ) return;
            if (shouldDisconnect()) {
                info("Non-friended player seen: {}", event.playerEntry().getProfile());
                Proxy.getInstance().disconnect(AUTO_DISCONNECT);
            }
        }
    }

    private boolean shouldDisconnect() {
        if (Proxy.getInstance().hasActivePlayer()) {
            var whilePlayerConnected = CONFIG.client.extra.utility.actions.autoDisconnect.whilePlayerConnected;
            if (!whilePlayerConnected)
                debug("Not disconnecting because a player is connected and whilePlayerConnected setting is disabled");
            return whilePlayerConnected;
        }
        return true;
    }
}
