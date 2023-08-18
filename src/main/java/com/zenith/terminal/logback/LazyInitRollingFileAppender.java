package com.zenith.terminal.logback;

import ch.qos.logback.core.rolling.RollingFileAppender;
import com.zenith.util.ImageInfo;

import java.util.concurrent.atomic.AtomicBoolean;

public class LazyInitRollingFileAppender<T> extends RollingFileAppender<T> {
    private final AtomicBoolean started = new AtomicBoolean(false);

    @Override
    public void start() {
        if (!ImageInfo.inImageBuildtimeCode()) {
            super.start();
            started.set(true);
        }
    }

    @Override
    public void doAppend(T eventObject) {
        if (!ImageInfo.inImageBuildtimeCode()) {
            if (!started.get()) {
                maybeStart();
            }
            super.doAppend(eventObject);
        }
    }

    private void maybeStart() {
        streamWriteLock.lock();
        try {
            if (!this.started.get()) {
                this.start();
            }
        } finally {
            streamWriteLock.unlock();
        }
    }
}
