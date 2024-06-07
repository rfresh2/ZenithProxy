package com.zenith.feature.autoupdater;

public class NoOpAutoUpdater extends AutoUpdater {
    public static NoOpAutoUpdater INSTANCE = new NoOpAutoUpdater();
    @Override
    public void updateCheck() {
        // :)
    }

    @Override
    public void start() {
        // :)
    }

    @Override
    public void stop() {
        // :)
    }
}
