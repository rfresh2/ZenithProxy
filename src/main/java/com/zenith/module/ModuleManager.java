package com.zenith.module;

import com.zenith.Proxy;
import com.zenith.event.Subscription;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.event.proxy.DisconnectEvent;
import com.zenith.event.proxy.PlayerOnlineEvent;
import com.zenith.event.proxy.ProxyClientConnectedEvent;
import com.zenith.event.proxy.ProxyClientDisconnectedEvent;
import com.zenith.module.impl.*;
import com.zenith.util.Wait;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.zenith.Shared.*;
import static com.zenith.event.SimpleEventBus.pair;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class ModuleManager {
    protected ScheduledFuture<?> clientTickFuture;
    private Subscription eventSubscription;
    private final Object2ObjectOpenHashMap<Class<? extends Module>, Module> moduleClassMap = new Object2ObjectOpenHashMap<>();

    public ModuleManager() {
        eventSubscription = EVENT_BUS.subscribe(
            pair(PlayerOnlineEvent.class, this::handlePlayerOnlineEvent),
            pair(ProxyClientConnectedEvent.class, this::handleProxyClientConnectedEvent),
            pair(ProxyClientDisconnectedEvent.class, this::handleProxyClientDisconnectedEvent),
            pair(DisconnectEvent.class, this::handleDisconnectEvent)
        );
    }

    public void init() {
        asList(
            new AntiAFK(),
            new AutoDisconnect(),
            new AutoEat(),
            new AutoReply(),
            new AutoRespawn(),
            new AutoTotem(),
            new KillAura(),
            new Spammer(),
            new Spook()
        ).forEach(m -> {
            addModule(m);
            m.syncEnabledFromConfig();
        });
    }

    private void addModule(Module module) {
        moduleClassMap.put(module.getClass(), module);
    }

    public <T extends Module> Optional<T> getModule(final Class<T> clazz) {
        Module module = moduleClassMap.get(clazz);
        if (module == null) {
            return Optional.empty();
        }
        try {
            return Optional.of((T) module);
        } catch (final ClassCastException e) {
            return Optional.empty();
        }
    }

    public List<Module> getModules() {
        return moduleClassMap.values().stream().toList();
    }

    public void handlePlayerOnlineEvent(final PlayerOnlineEvent event) {
        if (Proxy.getInstance().getActiveConnections().isEmpty()) {
            startClientTicks();
        }
    }

    public void handleProxyClientConnectedEvent(final ProxyClientConnectedEvent event) {
        stopClientTicks();
    }

    public void handleProxyClientDisconnectedEvent(final ProxyClientDisconnectedEvent event) {
        if (nonNull(Proxy.getInstance().getClient()) && Proxy.getInstance().getClient().isOnline()) {
            startClientTicks();
        }
    }

    public void handleDisconnectEvent(final DisconnectEvent event) {
        stopClientTicks();
    }

    public void startClientTicks() {
        synchronized (this) {
            if (isNull(clientTickFuture) || clientTickFuture.isDone()) {
                getModules().forEach(Module::clientTickStarting);
                clientTickFuture = SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(() -> {
                    if (Proxy.getInstance().isConnected()) {
                        try {
                            EVENT_BUS.post(new ClientTickEvent());
                        } catch (final Exception e) {
                            CLIENT_LOG.error("Client Tick Error", e);
                        }
                    }
                }, 0, 50L, TimeUnit.MILLISECONDS);
            }
        }
    }

    public synchronized void stopClientTicks() {
        synchronized (this) {
            if (nonNull(this.clientTickFuture)) {
                this.clientTickFuture.cancel(true);
                getModules().forEach(Module::clientTickStopping);
                while (!this.clientTickFuture.isDone()) {
                    Wait.waitALittleMs(50);
                }
            }
        }
    }
}
