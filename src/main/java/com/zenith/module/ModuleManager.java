package com.zenith.module;

import com.collarmc.pounce.Subscribe;
import com.zenith.Proxy;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.event.proxy.DisconnectEvent;
import com.zenith.event.proxy.PlayerOnlineEvent;
import com.zenith.event.proxy.ProxyClientConnectedEvent;
import com.zenith.event.proxy.ProxyClientDisconnectedEvent;
import com.zenith.util.Wait;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.zenith.util.Constants.*;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class ModuleManager {
    protected ScheduledFuture<?> clientTickFuture;
    protected final List<Module> modules;

    public ModuleManager() {
        EVENT_BUS.subscribe(this);
        this.modules = asList(
//                new AntiAFK(),
                new AutoDisconnect(),
                new AutoReply(),
                new Spook(),
                new AutoRespawn(),
//                new AutoTotem(),
                new AutoEat()
//                new KillAura()
        );
    }

    public <T> Optional<T> getModule(final Class<T> clazz) {
        return this.modules.stream()
                .filter(module -> module.getClass().equals(clazz))
                .map(module -> (T) module)
                .findFirst();
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
                this.modules.forEach(Module::clientTickStarting);
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
                this.modules.forEach(Module::clientTickStopping);
                while (!this.clientTickFuture.isDone()) {
                    Wait.waitALittleMs(50);
                }
            }
        }
    }
}
