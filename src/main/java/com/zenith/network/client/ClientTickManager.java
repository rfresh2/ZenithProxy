package com.zenith.network.client;

import com.zenith.Proxy;
import com.zenith.event.module.ClientBotTick;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.event.proxy.DisconnectEvent;
import com.zenith.event.proxy.PlayerOnlineEvent;
import com.zenith.event.proxy.ProxyClientConnectedEvent;
import com.zenith.event.proxy.ProxyClientDisconnectedEvent;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;
import static java.util.Objects.nonNull;

public class ClientTickManager {
    protected ScheduledFuture<?> clientTickFuture;
    private final AtomicBoolean doBotTicks = new AtomicBoolean(false);

    public ClientTickManager() {
        EVENT_BUS.subscribe(
            this,
            of(PlayerOnlineEvent.class, this::handlePlayerOnlineEvent),
            of(ProxyClientConnectedEvent.class, this::handleProxyClientConnectedEvent),
            of(ProxyClientDisconnectedEvent.class, this::handleProxyClientDisconnectedEvent),
            of(DisconnectEvent.class, this::handleDisconnectEvent)
        );
    }

    public void handlePlayerOnlineEvent(final PlayerOnlineEvent event) {
        if (!Proxy.getInstance().hasActivePlayer()) {
            startBotTicks();
        }
    }

    public void handleDisconnectEvent(final DisconnectEvent event) {
        stopBotTicks();
    }

    public void handleProxyClientConnectedEvent(final ProxyClientConnectedEvent event) {
        stopBotTicks();
    }

    public void handleProxyClientDisconnectedEvent(final ProxyClientDisconnectedEvent event) {
        if (nonNull(Proxy.getInstance().getClient()) && Proxy.getInstance().getClient().isOnline()) {
            startBotTicks();
        }
    }

    public synchronized void startClientTicks() {
        if (this.clientTickFuture == null || this.clientTickFuture.isDone()) {
            CLIENT_LOG.debug("Starting Client Ticks");
            EVENT_BUS.post(ClientTickEvent.Starting.INSTANCE);
            var eventLoop = Proxy.getInstance().getClient().getClientEventLoop();
            this.clientTickFuture = CONFIG.debug.clientTickFixedRate
                ? eventLoop.scheduleAtFixedRate(tickRunnable, 0, 50, TimeUnit.MILLISECONDS)
                : eventLoop.scheduleWithFixedDelay(tickRunnable, 0, 50, TimeUnit.MILLISECONDS);
        }
    }

    private final Runnable tickRunnable = () -> {
        try {
            EVENT_BUS.post(ClientTickEvent.INSTANCE);
            if (doBotTicks.get()) {
                EVENT_BUS.post(ClientBotTick.INSTANCE);
            }
        } catch (final Throwable e) {
            CLIENT_LOG.error("Error during client tick", e);
        }
    };

    public synchronized void stopClientTicks() {
        if (this.clientTickFuture != null && !this.clientTickFuture.isDone()) {
            this.clientTickFuture.cancel(false);
            try {
                this.clientTickFuture.get(1L, TimeUnit.SECONDS);
            } catch (final Exception e) {
                // fall through
            }
            if (doBotTicks.compareAndExchange(true, false)) {
                CLIENT_LOG.debug("Stopped Bot Ticks");
                EVENT_BUS.post(ClientBotTick.Stopped.INSTANCE);
            }
            CLIENT_LOG.debug("Stopped Client Ticks");
            EVENT_BUS.post(ClientTickEvent.Stopped.INSTANCE);
            this.clientTickFuture = null;
        }
    }

    public void startBotTicks() {
        if (doBotTicks.compareAndSet(false, true)) {
            CLIENT_LOG.debug("Starting Bot Ticks");
            EVENT_BUS.post(ClientBotTick.Starting.INSTANCE);
        }
    }

    public void stopBotTicks() {
        if (doBotTicks.compareAndSet(true, false)) {
            Proxy.getInstance().getClient().getClientEventLoop().execute(() -> {
                CLIENT_LOG.debug("Stopped Bot Ticks");
                EVENT_BUS.post(ClientBotTick.Stopped.INSTANCE);
            });
        }
    }
}
