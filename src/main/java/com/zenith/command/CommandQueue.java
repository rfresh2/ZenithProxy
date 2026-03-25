package com.zenith.command;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CommandQueue implements AutoCloseable {
    private static final ComponentLogger LOG = ComponentLogger.logger("CommandQueue");
    private final ThreadPoolExecutor executor;
    private final int maxPendingCommands;

    public CommandQueue(final String threadNameFormat, final int maxPendingCommands) {
        if (maxPendingCommands <= 0) {
            throw new IllegalArgumentException("maxPendingCommands must be greater than 0");
        }
        this.maxPendingCommands = maxPendingCommands;
        this.executor = new ThreadPoolExecutor(
            1,
            1,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder()
                .setNameFormat(threadNameFormat)
                .setDaemon(true)
                .setUncaughtExceptionHandler((thread, e) -> LOG.error("Uncaught exception in queued command thread {}", thread, e))
                .build());
    }

    public synchronized Submission submit(final Runnable runnable) {
        final int commandsAhead = executor.getQueue().size() + executor.getActiveCount();
        if (executor.getQueue().size() >= maxPendingCommands) {
            return new Submission(false, commandsAhead);
        }
        try {
            executor.execute(runnable);
            return new Submission(true, commandsAhead);
        } catch (final RejectedExecutionException e) {
            return new Submission(false, commandsAhead);
        }
    }

    public synchronized int clearPending() {
        final int cleared = executor.getQueue().size();
        executor.getQueue().clear();
        return cleared;
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    public record Submission(boolean accepted, int commandsAhead) { }
}
