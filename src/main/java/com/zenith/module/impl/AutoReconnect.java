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
    public boolean enabledSetting() {
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
        if (shouldAutoDisconnectCancelAutoReconnect(event)) {
            info("Cancelling AutoReconnect due to AutoDisconnect cancelAutoReconnect");
            return;
        }
        if (isReconnectableDisconnect(event.reason())) {
            scheduleAutoReconnect(CONFIG.client.extra.autoReconnect.delaySeconds);
        } else {
            info("Cancelling AutoReconnect because disconnect reason is not reconnectable");
        }
    }

    public void scheduleAutoReconnect(final int delaySeconds) {
        if (autoReconnectIsInProgress()) {
            info("AutoReconnect already in progress, not starting another");
            return;
        }
        this.autoReconnectFuture = EXECUTOR.submit(() -> autoReconnectRunnable(delaySeconds));
    }

    public boolean shouldAutoDisconnectCancelAutoReconnect(DisconnectEvent event) {
        return CONFIG.client.extra.utility.actions.autoDisconnect.enabled && CONFIG.client.extra.utility.actions.autoDisconnect.cancelAutoReconnect && AUTO_DISCONNECT.equals(event.reason());
    }

    public void handleConnectEvent(ConnectEvent event) {
        cancelAutoReconnect();
    }

    private void autoReconnectRunnable(int delaySeconds) {
        try {
            delayBeforeReconnect(delaySeconds);
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

    private void delayBeforeReconnect(int delaySeconds) {
        final int countdown = delaySeconds;
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
