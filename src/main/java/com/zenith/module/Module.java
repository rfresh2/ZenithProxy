package com.zenith.module;

import com.zenith.Proxy;
import com.zenith.network.client.ClientSession;
import org.geysermc.mcprotocollib.network.packet.Packet;

import static com.zenith.Shared.EVENT_BUS;

/**
 * Module system base class.
 */
public abstract class Module {
    boolean enabled = false;

    public Module() {

    }

    public synchronized void enable() {
        if (!enabled) {
            subscribeEvents();
            enabled = true;
            onEnable();
        }
    }

    public synchronized void disable() {
        if (enabled) {
            enabled = false;
            EVENT_BUS.unsubscribe(this);
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
        setEnabled(shouldBeEnabled());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void onEnable() { }

    public void onDisable() { }

    public abstract void subscribeEvents();

    public abstract boolean shouldBeEnabled();

    public void sendClientPacketAsync(final Packet packet) {
        ClientSession clientSession = Proxy.getInstance().getClient();
        if (clientSession != null && clientSession.isConnected()) {
            clientSession.sendAsync(packet);
        }
    }

    // preserves packet order
    public void sendClientPacketsAsync(final Packet... packets) {
        ClientSession clientSession = Proxy.getInstance().getClient();
        if (clientSession != null && clientSession.isConnected()) {
            for (Packet packet : packets) {
                clientSession.sendAsync(packet);
            }
        }
    }
}
