package com.zenith.module;

import com.zenith.Proxy;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.event.proxy.DisconnectEvent;
import com.zenith.event.proxy.PlayerOnlineEvent;
import com.zenith.event.proxy.ProxyClientConnectedEvent;
import com.zenith.event.proxy.ProxyClientDisconnectedEvent;
import com.zenith.module.impl.*;
import com.zenith.util.Wait;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class ModuleManager {
    protected ScheduledFuture<?> clientTickFuture;
    private final Reference2ObjectMap<Class<? extends Module>, Module> moduleClassMap = new Reference2ObjectOpenHashMap<>();

    public ModuleManager() {
        EVENT_BUS.subscribe(this,
                            of(PlayerOnlineEvent.class, this::handlePlayerOnlineEvent),
                            of(ProxyClientConnectedEvent.class, this::handleProxyClientConnectedEvent),
                            of(ProxyClientDisconnectedEvent.class, this::handleProxyClientDisconnectedEvent),
                            of(DisconnectEvent.class, this::handleDisconnectEvent)
        );
    }

    public void init() {
        asList(
            new ActionLimiter(),
            new AntiAFK(),
            new AntiKick(),
            new AntiLeak(),
            new AutoDisconnect(),
            new AutoEat(),
            new AutoFish(),
            new AutoReply(),
            new AutoRespawn(),
            new AutoTotem(),
            new KillAura(),
            new PlayerSimulation(),
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

    public <T extends Module> T get(final Class<T> clazz) {
        try {
            return (T) moduleClassMap.get(clazz);
        } catch (final Throwable e) {
            return null;
        }
    }

    public List<Module> getModules() {
        return moduleClassMap.values().stream().toList();
    }

    public void handlePlayerOnlineEvent(final PlayerOnlineEvent event) {
        if (!Proxy.getInstance().hasActivePlayer()) {
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
                            EVENT_BUS.post(ClientTickEvent.INSTANCE);
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
                this.clientTickFuture.cancel(false);
                Wait.waitUntil(() -> this.clientTickFuture.isDone(), 1);
                getModules().forEach(Module::clientTickStopped);
            }
        }
    }
}
