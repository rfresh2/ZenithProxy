package com.zenith.module.impl;

import com.zenith.Proxy;
import com.zenith.event.module.PlayerHealthChangedEvent;
import com.zenith.event.module.WeatherChangeEvent;
import com.zenith.event.proxy.HealthAutoDisconnectEvent;
import com.zenith.event.proxy.NewPlayerInVisualRangeEvent;
import com.zenith.event.proxy.ProxyClientDisconnectedEvent;
import com.zenith.module.Module;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;
import static java.util.Objects.nonNull;

public class AutoDisconnect extends Module {

    public AutoDisconnect() {
        super();
    }

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(
            this,
            of(PlayerHealthChangedEvent.class, this::handleLowPlayerHealthEvent),
            of(WeatherChangeEvent.class, this::handleWeatherChangeEvent),
            of(ProxyClientDisconnectedEvent.class, this::handleProxyClientDisconnectedEvent),
            of(NewPlayerInVisualRangeEvent.class, this::handleNewPlayerInVisualRangeEvent)
        );
    }

    @Override
    public boolean shouldBeEnabled() {
        return CONFIG.client.extra.utility.actions.autoDisconnect.enabled;
    }

    public void handleLowPlayerHealthEvent(final PlayerHealthChangedEvent event) {
        if (!CONFIG.client.extra.utility.actions.autoDisconnect.enabled) return;
        if (event.newHealth() <= CONFIG.client.extra.utility.actions.autoDisconnect.health
            && playerConnectedCheck()) {
            info("Health disconnect: {} < {}",
                 event.newHealth(),
                 CONFIG.client.extra.utility.actions.autoDisconnect.health);
            EVENT_BUS.postAsync(new HealthAutoDisconnectEvent());
            Proxy.getInstance().disconnect(AUTO_DISCONNECT);
        }
    }

    public void handleWeatherChangeEvent(final WeatherChangeEvent event) {
        if (!CONFIG.client.extra.utility.actions.autoDisconnect.thunder) return;
        if (CACHE.getChunkCache().isRaining()
            && CACHE.getChunkCache().getThunderStrength() > 0.0f
            && playerConnectedCheck()) {
            Proxy.getInstance().disconnect(AUTO_DISCONNECT);
        }
    }

    public void handleProxyClientDisconnectedEvent(ProxyClientDisconnectedEvent event) {
        if (!CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect) return;
        var connection = Proxy.getInstance().getActivePlayer();
        if (nonNull(connection) && connection.getProfileCache().getProfile().equals(event.clientGameProfile())) {
            info("Auto Client Disconnect");
            Proxy.getInstance().disconnect();
        }
    }

    public void handleNewPlayerInVisualRangeEvent(NewPlayerInVisualRangeEvent event) {
        if (!CONFIG.client.extra.utility.actions.autoDisconnect.onUnknownPlayerInVisualRange) return;
        var playerUUID = event.playerEntity().getUuid();
        if (PLAYER_LISTS.getFriendsList().contains(playerUUID)
            || PLAYER_LISTS.getWhitelist().contains(playerUUID)
            || PLAYER_LISTS.getSpectatorWhitelist().contains(playerUUID)
            || !playerConnectedCheck()
        ) return;
        info("Unknown player: {} [{}]", event.playerEntry().getProfile());
        Proxy.getInstance().disconnect(AUTO_DISCONNECT);
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
}
