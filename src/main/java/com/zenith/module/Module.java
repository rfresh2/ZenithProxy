package com.zenith.module;

import com.zenith.Proxy;
import com.zenith.network.client.ClientSession;
import lombok.Getter;
import org.geysermc.mcprotocollib.network.packet.Packet;

import static com.zenith.Shared.EVENT_BUS;
import static com.zenith.Shared.MODULE_LOG;

/**
 * Module system base class.
 */
@Getter
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

    private final String moduleLogPrefix = "[" + this.getClass().getSimpleName() + "] ";

    public void info(String msg) {
        MODULE_LOG.info(moduleLogPrefix + msg);
    }

    public void info(String msg, Object... args) {
        MODULE_LOG.info(moduleLogPrefix + msg, args);
    }

    public void error(String msg) {
        MODULE_LOG.error(moduleLogPrefix + msg);
    }

    public void error(String msg, Object... args) {
        MODULE_LOG.error(moduleLogPrefix + msg, args);
    }

    public void debug(String msg) {
        MODULE_LOG.debug(moduleLogPrefix + msg);
    }

    public void debug(String msg, Object... args) {
        MODULE_LOG.debug(moduleLogPrefix + msg, args);
    }

    public void warn(String msg) {
        MODULE_LOG.warn(msg);
    }

    public void warn(String msg, Object... args) {
        MODULE_LOG.warn(moduleLogPrefix + msg, args);
    }

    public void inGameAlert(String minedown) {
        var connections = Proxy.getInstance().getActiveConnections().getArray();
        for (int i = 0; i < connections.length; i++) {
            var connection = connections[i];
            connection.sendAsyncAlert(minedown);
        }
    }
}
