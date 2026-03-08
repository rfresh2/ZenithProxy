package com.zenith.util;

import java.util.concurrent.*;

public class ZenithScheduledExecutor extends ScheduledThreadPoolExecutor {
    public ZenithScheduledExecutor(int corePoolSize, ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory);
    }

    @Override
    protected void afterExecute(final Runnable runnable, final Throwable throwable) {
        super.afterExecute(runnable, throwable);
        var error = throwable;
        if (error == null && runnable instanceof Future<?> future && future.isDone()) {
            try {
                future.get();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (final CancellationException ignored) {
                return;
            } catch (final ExecutionException e) {
                error = e.getCause();
            }
        }
        if (error == null) return;
        var thread = Thread.currentThread();
        var handler = thread.getUncaughtExceptionHandler();
        if (handler != null) {
            handler.uncaughtException(thread, error);
        }
    }
}
