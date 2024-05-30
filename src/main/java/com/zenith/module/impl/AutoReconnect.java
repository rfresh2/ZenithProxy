package com.zenith.module.impl;

import com.zenith.Proxy;
import com.zenith.event.proxy.AutoReconnectEvent;
import com.zenith.event.proxy.ConnectEvent;
import com.zenith.event.proxy.DisconnectEvent;
import com.zenith.module.Module;
import com.zenith.util.Wait;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Future;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;

public class AutoReconnect extends Module {
    private @Nullable Future<?> autoReconnectFuture;

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(
            this,
            of(DisconnectEvent.class, this::handleDisconnectEvent),
            of(ConnectEvent.class, this::handleConnectEvent)
        );
    }

    @Override
    public boolean shouldBeEnabled() {
        return CONFIG.client.extra.autoReconnect.enabled;
    }

    public boolean cancelAutoReconnect() {
        if (autoReconnectIsInProgress()) {
            var future = this.autoReconnectFuture;
            this.autoReconnectFuture = null;
            if (future != null) future.cancel(true);
            return true;
        }
        return false;
    }

    public boolean autoReconnectIsInProgress() {
        return this.autoReconnectFuture != null && !this.autoReconnectFuture.isDone();
    }

    @Override
    public void onDisable() {
        cancelAutoReconnect();
    }

    public void handleDisconnectEvent(DisconnectEvent event) {
        if (!CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect) {
            // skip autoreconnect when we want to sync client disconnect
            if (CONFIG.client.extra.autoReconnect.enabled && isReconnectableDisconnect(event.reason())) {
                if (autoReconnectIsInProgress()) return;
                this.autoReconnectFuture = EXECUTOR.submit(this::autoReconnectRunnable);
            }
        }
    }

    public void handleConnectEvent(ConnectEvent event) {
        cancelAutoReconnect();
    }

    private void autoReconnectRunnable() {
        try {
            delayBeforeReconnect();
            if (Thread.currentThread().isInterrupted()) return;
            EXECUTOR.execute(() -> {
                try {
                    Proxy.getInstance().connect();
                } catch (final Throwable e) {
                    DEFAULT_LOG.error("Error connecting", e);
                }
            });
            this.autoReconnectFuture = null;
        } catch (final Exception e) {
            info("AutoReconnect stopped");
        }
    }

    private void delayBeforeReconnect() {
        final int countdown = CONFIG.client.extra.autoReconnect.delaySeconds;
        EVENT_BUS.postAsync(new AutoReconnectEvent(countdown));
        // random jitter to help prevent multiple clients from logging in at the same time
        Wait.wait((((int) (Math.random() * 5))) % 10);
        for (int i = countdown; i > 0; i-=10) {
            info("Reconnecting in {}s", i);
            Wait.wait(10);
        }
    }

    private boolean isReconnectableDisconnect(final String reason) {
        if (reason.equals(SYSTEM_DISCONNECT)
            || reason.equals(MANUAL_DISCONNECT)
            || reason.equals(MinecraftConstants.SERVER_CLOSING_MESSAGE)
            || reason.equals(LOGIN_FAILED)
        ) {
            return false;
        } else if (reason.equals(AUTO_DISCONNECT)) {
            return (!CONFIG.client.extra.utility.actions.autoDisconnect.cancelAutoReconnect && !Proxy.getInstance().isPrio());
        } else {
            return true;
        }
    }
}
