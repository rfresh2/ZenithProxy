package com.zenith.util;

import com.zenith.Proxy;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class RequestFuture implements Future<Boolean> {
    // whether the future has completed
    @Getter
    private volatile boolean completed = false;
    // whether this request was accepted
    @Getter
    private volatile boolean accepted = false;

    public static final RequestFuture rejected = immediateFuture(false);

    public static RequestFuture immediateFuture(final boolean accepted) {
        final RequestFuture future = new RequestFuture();
        future.complete(accepted);
        return future;
    }

    protected void setCompleted(final boolean completed) {
        if (this.completed) return;
        this.completed = completed;
    }

    protected void setAccepted(final boolean accepted) {
        if (completed) return;
        this.accepted = accepted;
    }

    public synchronized void complete(final boolean accepted) {
        setAccepted(accepted);
        setCompleted(true);
    }

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException("RequestFuture cannot be cancelled");
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return isCompleted();
    }

    @SneakyThrows
    @Override
    public Boolean get() {
        var client = Proxy.getInstance().getClient();
        if (client != null && client.getClientEventLoop().inEventLoop()) {
            throw new IllegalStateException("Cannot block on RequestFuture in client event loop");
        }
        Wait.waitUntil(this::isCompleted, 1, 1L, TimeUnit.SECONDS);
        return isAccepted();
    }

    @SneakyThrows
    @Override
    public Boolean get(final long timeout, @NonNull final TimeUnit unit) {
        var client = Proxy.getInstance().getClient();
        if (client != null && client.getClientEventLoop().inEventLoop()) {
            throw new IllegalStateException("Cannot block on RequestFuture in client event loop");
        }
        Wait.waitUntil(this::isCompleted, 1, timeout, unit);
        return isAccepted();
    }

    public boolean getNow() {
        if (!isCompleted()) return false;
        return isAccepted();
    }
}
