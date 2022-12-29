package com.zenith.module;

import com.collarmc.pounce.Subscribe;
import com.zenith.Proxy;
import com.zenith.event.module.PlayerHealthChangedEvent;
import com.zenith.event.module.WeatherChangeEvent;
import com.zenith.event.proxy.HealthAutoDisconnectEvent;
import com.zenith.event.proxy.ProxyClientDisconnectedEvent;
import com.zenith.server.ServerConnection;
import com.zenith.util.Constants;

import static com.zenith.util.Constants.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class AutoDisconnect extends Module {
    public AutoDisconnect(Proxy proxy) {
        super(proxy);
    }

    @Subscribe
    public void handleLowPlayerHealthEvent(final PlayerHealthChangedEvent event) {
        if (Constants.CONFIG.client.extra.utility.actions.autoDisconnect.enabled) {
            if (event.newHealth <= Constants.CONFIG.client.extra.utility.actions.autoDisconnect.health) {
                if (isNull(this.proxy.getCurrentPlayer().get())) {
                    EVENT_BUS.dispatch(new HealthAutoDisconnectEvent());
                    this.proxy.disconnect();
                }
            }
        }
    }

    @Subscribe
    public void handleWeatherChangeEvent(final WeatherChangeEvent event) {
        synchronized (this) {
            if (CACHE.getChunkCache().isRaining() && CACHE.getChunkCache().getThunderStrength() > 0.0f) {
                if (isNull(this.proxy.getCurrentPlayer().get())) {
                    this.proxy.disconnect("Thunder AutoDisconnect");
                }
            }
        }
    }

    @Subscribe
    public void handleProxyClientDisconnectedEvent(ProxyClientDisconnectedEvent event) {
        if (CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect) {
            ServerConnection currentConnection = this.proxy.getCurrentPlayer().get();
            if (nonNull(currentConnection) && currentConnection.getProfileCache().getProfile().equals(event.clientGameProfile)) {
                this.proxy.disconnect();
            }
        }
    }
}
