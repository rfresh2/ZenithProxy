package com.zenith.feature.autoupdater;

import com.collarmc.pounce.Subscribe;
import com.zenith.Proxy;
import com.zenith.event.proxy.DisconnectEvent;
import com.zenith.event.proxy.UpdateStartEvent;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.TrackingRefUpdate;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.zenith.Shared.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class AutoUpdater {
    // todo: design an autoupdater for native image.
    //  embed version identifier in native image. we already embed the short commit hash
    //      also embed a release channel?
    //  wget binary from github releases if commit hash doesn't match?
    private ScheduledExecutorService updaterExecutorService;
    private Git git;
    private boolean updateAvailable = false;

    public AutoUpdater() {
        EVENT_BUS.subscribe(this);
    }

    public boolean start() {
        if (isNull(git)) { // lazily init
            try {
                git = Git.open(new File(System.getProperty("user.dir")));
            } catch (IOException e) {
                DEFAULT_LOG.error("Error starting AutoUpdater", e);
                return false;
            }
        }
        if (isNull(updaterExecutorService) || updaterExecutorService.isShutdown()) {
            updaterExecutorService = new ScheduledThreadPoolExecutor(1);
            updaterExecutorService.scheduleWithFixedDelay(this::updateCheck, 3, CONFIG.autoUpdater.autoUpdateCheckIntervalSeconds, TimeUnit.SECONDS);
        }
        return true;
    }

    private void updateCheck() {
        try {
            final FetchResult fetchResult = git.fetch().setCheckFetchedObjects(true).setDryRun(true).setTimeout(30).call();
            List<TrackingRefUpdate> localBranchUpdates = fetchResult.getTrackingRefUpdates().stream()
                    .filter(trackingRefUpdate -> {
                        try {
                            return Objects.equals(trackingRefUpdate.getRemoteName(), git.getRepository().getFullBranch());
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
            if (localBranchUpdates.size() > 0) {
                if (!updateAvailable) DEFAULT_LOG.info("New update found!"); // only log on first detection
                updateAvailable = true;
                if (!Proxy.getInstance().isConnected()
                        // adding some delay here to prefer disconnect event updates if times happen to align
                        && Proxy.getInstance().getDisconnectTime().isBefore(Instant.now().minus(60L, ChronoUnit.SECONDS))) {
                    update();
                }
            } else {
                updateAvailable = false;
            }
        } catch (final Throwable e) {
            DEFAULT_LOG.error("Error checking for updates.", e);
            // fall through
            updateAvailable = false;
        }
    }

    public void stop() {
        if (nonNull(this.updaterExecutorService)) this.updaterExecutorService.shutdownNow();
        this.updateAvailable = false;
    }

    @Subscribe
    public void handleDisconnectEvent(final DisconnectEvent event) {
        if (CONFIG.autoUpdater.autoUpdate && updateAvailable) {
            if (!event.reason.equals(MANUAL_DISCONNECT)) {
                CONFIG.autoUpdater.shouldReconnectAfterAutoUpdate = true;
            } else {
                CONFIG.autoUpdater.shouldReconnectAfterAutoUpdate = false;
            }
            saveConfig();
            scheduleConditionalUpdate();
        }
    }

    private void scheduleConditionalUpdate() {
        if (Proxy.getInstance().getIsPrio().orElse(CONFIG.authentication.prio)) {
            // update immediately if we have prio
            update();
        } else {
            updaterExecutorService.schedule(this::conditionalRegularQueueUpdate, 30L, TimeUnit.SECONDS);
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
        EVENT_BUS.dispatch(new UpdateStartEvent());
        CONFIG.discord.isUpdating = true;
        Proxy.getInstance().stop();
    }
}
