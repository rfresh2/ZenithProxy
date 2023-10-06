package com.zenith.feature.autoupdater;

import com.zenith.Proxy;
import com.zenith.event.Subscription;
import com.zenith.event.proxy.DisconnectEvent;
import com.zenith.event.proxy.UpdateAvailableEvent;
import com.zenith.event.proxy.UpdateStartEvent;
import lombok.Getter;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.zenith.Shared.*;

public abstract class AutoUpdater {

    private boolean updateAvailable = false;
    @Getter
    private Optional<String> newVersion = Optional.empty();
    ScheduledFuture<?> updateCheckFuture;
    private Subscription eventSubscription;

    public void start() {
        if (updateCheckFuture != null) return;
        if (eventSubscription == null) eventSubscription = EVENT_BUS.subscribe(
            DisconnectEvent.class, this::handleDisconnectEvent
        );
        scheduleUpdateCheck(this::executeUpdateCheck,
                            30 + ThreadLocalRandom.current().nextInt(150),
                            Math.max(CONFIG.autoUpdater.autoUpdateCheckIntervalSeconds, 300),
                            TimeUnit.SECONDS);
    }

    public void scheduleUpdateCheck(Runnable runnable, long initialDelay, long interval, TimeUnit timeUnit) {
        updateCheckFuture = SCHEDULED_EXECUTOR_SERVICE
            .scheduleWithFixedDelay(runnable,
                                    initialDelay,
                                    interval,
                                    timeUnit);
    }

    public void executeUpdateCheck() {
        try {
            updateCheck();
        } catch (final Throwable e) {
            DEFAULT_LOG.error("Error performing auto-updater update check", e);
        }
    }

    public abstract void updateCheck();

    public void cancelUpdateCheck() {
        if (updateCheckFuture != null) {
            updateCheckFuture.cancel(true);
        }
    }

    public void stop() {
        if (eventSubscription != null) {
            eventSubscription.unsubscribe();
            eventSubscription = null;
        }
        cancelUpdateCheck();
        this.updateAvailable = false;
        this.newVersion = Optional.empty();
    }

    public synchronized void setUpdateAvailable(final boolean updateAvailable) {
        setUpdateAvailable(updateAvailable, null);
    }

    public synchronized void setUpdateAvailable(final boolean updateAvailable, @Nullable final String version) {
        if (!this.updateAvailable && updateAvailable) EVENT_BUS.postAsync(new UpdateAvailableEvent(version));
        this.newVersion = Optional.ofNullable(version);
        this.updateAvailable = updateAvailable;
        if (this.updateAvailable) {
            if (!Proxy.getInstance().isConnected()
                // adding some delay here to prefer disconnect event updates if times happen to align
                && Proxy.getInstance().getDisconnectTime().isBefore(Instant.now().minus(60L, ChronoUnit.SECONDS))) {
                update();
            }
        }
    }

    public synchronized boolean getUpdateAvailable() {
        return updateAvailable;
    }

    public void handleDisconnectEvent(final DisconnectEvent event) {
        if (updateAvailable && !CONFIG.discord.isUpdating) {
            CONFIG.autoUpdater.shouldReconnectAfterAutoUpdate = !event.reason.equals(MANUAL_DISCONNECT);
            saveConfigAsync();
            scheduleConditionalUpdate();
        }
    }

    private void scheduleConditionalUpdate() {
        if (Proxy.getInstance().getIsPrio().orElse(CONFIG.authentication.prio)) {
            // update immediately if we have prio
            update();
        } else {
            SCHEDULED_EXECUTOR_SERVICE.schedule(this::conditionalRegularQueueUpdate, 30L, TimeUnit.SECONDS);
        }
    }

    private void conditionalRegularQueueUpdate() {
        if (Proxy.getInstance().isConnected()) {
            // queue skipped
            if (!Proxy.getInstance().isInQueue()) return;
            // we're in the middle of a queue skip
            if (Proxy.getInstance().getQueuePosition() < 10) return;
        }
        update();
    }

    public void update() {
        EVENT_BUS.post(new UpdateStartEvent(newVersion));
        CONFIG.discord.isUpdating = true;
        Proxy.getInstance().stop();
    }
}
