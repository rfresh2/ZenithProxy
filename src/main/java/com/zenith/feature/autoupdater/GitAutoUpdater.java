package com.zenith.feature.autoupdater;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.TrackingRefUpdate;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.zenith.Shared.DEFAULT_LOG;
import static java.util.Objects.isNull;

public class GitAutoUpdater extends AutoUpdater {
    private Git git;

    public GitAutoUpdater() {

    }

    @Override
    public void start() {
        if (isNull(git)) { // lazily init
            try {
                git = Git.open(new File(System.getProperty("user.dir")));
            } catch (IOException e) {
                DEFAULT_LOG.error("Error starting AutoUpdater", e);
            }
        }
        super.start();
    }

    @Override
    public void updateCheck() {
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
                setUpdateAvailable(true);
            } else {
                setUpdateAvailable(false);
            }
        } catch (final Throwable e) {
            DEFAULT_LOG.error("Error checking for updates.", e);
            // fall through
            setUpdateAvailable(false);
        }
    }
}
