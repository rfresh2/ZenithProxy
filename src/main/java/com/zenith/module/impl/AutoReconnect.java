package com.zenith.module.impl;

import com.github.rfresh2.EventConsumer;
import com.google.common.collect.Sets;
import com.zenith.Proxy;
import com.zenith.event.client.ClientConnectEvent;
import com.zenith.event.client.ClientDisconnectEvent;
import com.zenith.event.module.AutoReconnectEvent;
import com.zenith.module.api.Module;
import com.zenith.util.Wait;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;
import static com.zenith.util.DisconnectMessages.*;

public class AutoReconnect extends Module {
    private @Nullable Future<?> autoReconnectFuture;

    @Override
    public List<EventConsumer<?>> registerEvents() {
        return List.of(
            of(ClientDisconnectEvent.class, this::handleDisconnectEvent),
            of(ClientConnectEvent.class, this::handleConnectEvent)
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

    public void handleDisconnectEvent(ClientDisconnectEvent event) {
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

    public boolean shouldAutoDisconnectCancelAutoReconnect(ClientDisconnectEvent event) {
        return CONFIG.client.extra.utility.actions.autoDisconnect.enabled
            && CONFIG.client.extra.utility.actions.autoDisconnect.cancelAutoReconnect
            && AutoDisconnect.isAutoDisconnectReason(event.reason());
    }

    public void handleConnectEvent(ClientConnectEvent event) {
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
        Wait.waitRandomMs(1000);
        for (int i = countdown; i > 0; i-=10) {
            info("Reconnecting in {}s", i);
            Wait.wait(Math.min(10, i));
        }
    }

    private static final Set<String> NON_RECONNECTABLE_DISCONNECT_REASONS = Sets.newHashSet(
        SYSTEM_DISCONNECT,
        MANUAL_DISCONNECT,
        SERVER_CLOSING_MESSAGE,
        LOGIN_FAILED,
        AUTH_REQUIRED,
        MAX_PT_DISCONNECT
    );

    private boolean isReconnectableDisconnect(final String reason) {
        if (NON_RECONNECTABLE_DISCONNECT_REASONS.contains(reason)) {
            return false;
        } else if (ActiveHours.isActiveHoursDisconnect(reason)) {
            return false;
        } else if (AutoDisconnect.isAutoDisconnectReason(reason)) {
            return (!CONFIG.client.extra.utility.actions.autoDisconnect.cancelAutoReconnect && !Proxy.getInstance().isPrio());
        } else {
            return true;
        }
    }
}
