package com.zenith.util;

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

import static com.zenith.util.Constants.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class AutoUpdater {
    private final Proxy proxy;
    private ScheduledExecutorService updaterExecutorService;
    private Git git;
    private boolean updateAvailable = false;

    public AutoUpdater(final Proxy proxy) {
        this.proxy = proxy;
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
            updaterExecutorService.scheduleAtFixedRate(this::updateCheck, 3, CONFIG.autoUpdateCheckIntervalSeconds, TimeUnit.SECONDS);
        }
        return true;
    }

    private void updateCheck() {
        try {
            final FetchResult fetchResult = git.fetch().setCheckFetchedObjects(true).setDryRun(true).call();
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
                if (!proxy.isConnected()
                        // adding some delay here to prefer disconnect event updates if times happen to align
                        && proxy.getDisconnectTime().isBefore(Instant.now().minus(60L, ChronoUnit.SECONDS))) {
                    update();
                }
            } else {
                updateAvailable = false;
            }
        } catch (final Exception e) {
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
        if (CONFIG.autoUpdate && updateAvailable) {
            if (!event.reason.equals(MANUAL_DISCONNECT)) {
                CONFIG.shouldReconnectAfterAutoUpdate = true;
                saveConfig();
            }
            update();
        }
    }

    public void update() {
        EVENT_BUS.dispatch(new UpdateStartEvent());
        CONFIG.discord.isUpdating = true;
        this.proxy.stop();
    }
}
