package com.zenith.module.impl;

import com.zenith.Proxy;
import com.zenith.Shared;
import com.zenith.event.Subscription;
import com.zenith.event.module.PlayerHealthChangedEvent;
import com.zenith.event.module.WeatherChangeEvent;
import com.zenith.event.proxy.HealthAutoDisconnectEvent;
import com.zenith.event.proxy.ProxyClientDisconnectedEvent;
import com.zenith.module.Module;
import com.zenith.network.server.ServerConnection;

import java.util.function.Consumer;

import static com.zenith.Shared.*;
import static com.zenith.util.Pair.of;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class AutoDisconnect extends Module {
    private Subscription eventSubscription;

    public AutoDisconnect() {
        super();
        this.eventSubscription = EVENT_BUS.subscribe(
            of(PlayerHealthChangedEvent.class, (Consumer<PlayerHealthChangedEvent>)this::handleLowPlayerHealthEvent),
            of(WeatherChangeEvent.class, (Consumer<WeatherChangeEvent>)this::handleWeatherChangeEvent),
            of(ProxyClientDisconnectedEvent.class, (Consumer<ProxyClientDisconnectedEvent>)this::handleProxyClientDisconnectedEvent)
        );
    }

    public void handleLowPlayerHealthEvent(final PlayerHealthChangedEvent event) {
        if (Shared.CONFIG.client.extra.utility.actions.autoDisconnect.enabled) {
            if (event.newHealth <= Shared.CONFIG.client.extra.utility.actions.autoDisconnect.health) {
                if (isNull(Proxy.getInstance().getCurrentPlayer().get())) {
                    EVENT_BUS.postAsync(new HealthAutoDisconnectEvent());
                    Proxy.getInstance().disconnect(AUTO_DISCONNECT);
                }
            }
        }
    }

    public void handleWeatherChangeEvent(final WeatherChangeEvent event) {
        if (CONFIG.client.extra.utility.actions.autoDisconnect.thunder) {
            synchronized (this) {
                if (CACHE.getChunkCache().isRaining() && CACHE.getChunkCache().getThunderStrength() > 0.0f) {
                    if (isNull(Proxy.getInstance().getCurrentPlayer().get())) {
                        Proxy.getInstance().disconnect(AUTO_DISCONNECT);
                    }
                }
            }
        }
    }

    public void handleProxyClientDisconnectedEvent(ProxyClientDisconnectedEvent event) {
        if (CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect) {
            ServerConnection currentConnection = Proxy.getInstance().getCurrentPlayer().get();
            if (nonNull(currentConnection) && currentConnection.getProfileCache().getProfile().equals(event.clientGameProfile)) {
                Proxy.getInstance().disconnect();
            }
        }
    }
}
