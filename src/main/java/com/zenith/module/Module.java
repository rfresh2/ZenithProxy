package com.zenith.module;

import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.Proxy;
import com.zenith.event.Subscription;
import com.zenith.network.client.ClientSession;

import javax.annotation.Nullable;
import java.util.function.Supplier;

import static com.zenith.Shared.SCHEDULED_EXECUTOR_SERVICE;

/**
 * Module system base class.
 */
public abstract class Module {

    @Nullable
    Subscription eventSubscription = null;
    boolean enabled = false;

    public Module() {

    }

    public synchronized void enable() {
        if (!enabled) {
            if (eventSubscription != null)
                eventSubscription.unsubscribe();
            eventSubscription = subscribeEvents();
            enabled = true;
            onEnable();
        }
    }

    public synchronized void disable() {
        if (enabled) {
            enabled = false;
            if (eventSubscription != null) {
                eventSubscription.unsubscribe();
                eventSubscription = null;
            }
            onDisable();
        }
    }

    public synchronized void setEnabled(boolean enabled) {
        if (enabled) {
            enable();
        } else {
            disable();
        }
    }

    public synchronized void syncEnabledFromConfig() {
        setEnabled(shouldBeEnabled().get());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void onEnable() { }

    public void onDisable() { }

    public abstract Subscription subscribeEvents();

    public abstract Supplier<Boolean> shouldBeEnabled();

    public void clientTickStarting() {
    }

    public void clientTickStopped() {
    }

    public void sendClientPacketAsync(final Packet packet) {
        SCHEDULED_EXECUTOR_SERVICE.execute(() -> {
            ClientSession clientSession = Proxy.getInstance().getClient();
            if (clientSession != null && clientSession.isConnected()) {
                clientSession.send(packet);
            }
        });
    }
}
