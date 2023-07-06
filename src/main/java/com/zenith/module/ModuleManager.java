package com.zenith.module;

import com.collarmc.pounce.Subscribe;
import com.zenith.Proxy;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.event.proxy.DisconnectEvent;
import com.zenith.event.proxy.PlayerOnlineEvent;
import com.zenith.event.proxy.ProxyClientConnectedEvent;
import com.zenith.event.proxy.ProxyClientDisconnectedEvent;
import com.zenith.util.ClassUtil;
import com.zenith.util.Wait;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.zenith.Shared.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class ModuleManager {
    protected ScheduledFuture<?> clientTickFuture;
    private static final String modulePackage = "com.zenith.module.impl";
    private final Object2ObjectOpenHashMap<Class<? extends Module>, Module> moduleClassMap = new Object2ObjectOpenHashMap<>();

    public ModuleManager() {
        EVENT_BUS.subscribe(this);
        init();
    }

    public void init() {
        for (final Class<?> clazz : ClassUtil.findClassesInPath(modulePackage)) {
            if (isNull(clazz)) continue;
            if (Module.class.isAssignableFrom(clazz)) {
                try {
                    final Module module = (Module) clazz.newInstance();
                    addModule(module);
                } catch (InstantiationException | IllegalAccessException e) {
                    MODULE_LOG.warn("Error initializing command class", e);
                }
            }
        }
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

    @Subscribe
    public void handlePlayerOnlineEvent(final PlayerOnlineEvent event) {
        if (Proxy.getInstance().getActiveConnections().isEmpty()) {
            startClientTicks();
        }
    }

    @Subscribe
    public void handleProxyClientConnectedEvent(final ProxyClientConnectedEvent event) {
        stopClientTicks();
    }

    @Subscribe
    public void handleProxyClientDisconnectedEvent(final ProxyClientDisconnectedEvent event) {
        if (nonNull(Proxy.getInstance().getClient()) && Proxy.getInstance().getClient().isOnline()) {
            startClientTicks();
        }
    }

    @Subscribe
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
                            EVENT_BUS.dispatch(new ClientTickEvent());
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
