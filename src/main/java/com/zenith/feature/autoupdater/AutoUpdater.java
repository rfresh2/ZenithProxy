package com.zenith.feature.autoupdater;

import com.zenith.Proxy;
import com.zenith.event.Subscription;
import com.zenith.event.proxy.DisconnectEvent;
import com.zenith.event.proxy.UpdateAvailableEvent;
import com.zenith.event.proxy.UpdateStartEvent;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.zenith.Shared.*;

public abstract class AutoUpdater {

    private boolean updateAvailable = false;
    ScheduledFuture<?> updateCheckFuture;
    private Subscription eventSubscription;

    public void start() {
        if (updateCheckFuture != null) return;
        if (eventSubscription != null) eventSubscription = EVENT_BUS.subscribe(
            DisconnectEvent.class, (Consumer<DisconnectEvent>)this::handleDisconnectEvent
        );
        scheduleUpdateCheck(this::updateCheck, 3, CONFIG.autoUpdater.autoUpdateCheckIntervalSeconds, TimeUnit.SECONDS);
    }

    public void scheduleUpdateCheck(Runnable runnable, long initialDelay, long interval, TimeUnit timeUnit) {
        updateCheckFuture = SCHEDULED_EXECUTOR_SERVICE
            .scheduleWithFixedDelay(runnable,
                                    initialDelay,
                                    interval,
                                    timeUnit);
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
    }

    public synchronized void setUpdateAvailable(final boolean updateAvailable) {
        setUpdateAvailable(updateAvailable, null);
    }

    public synchronized void setUpdateAvailable(final boolean updateAvailable, @Nullable final String version) {
        if (!this.updateAvailable && updateAvailable) EVENT_BUS.postAsync(new UpdateAvailableEvent(version));
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
        if (updateAvailable) {
            CONFIG.autoUpdater.shouldReconnectAfterAutoUpdate = !event.reason.equals(MANUAL_DISCONNECT);
            saveConfig();
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
        EVENT_BUS.post(new UpdateStartEvent());
        CONFIG.discord.isUpdating = true;
        Proxy.getInstance().stop();
    }
}
