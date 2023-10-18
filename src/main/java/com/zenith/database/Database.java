package com.zenith.database;

import com.zenith.event.Subscription;

public abstract class Database {
    protected final QueryExecutor queryExecutor;
    private Subscription eventSubscription;
    boolean isRunning = false;

    public Database(final QueryExecutor queryExecutor) {
        this.queryExecutor = queryExecutor;
    }

    public void start() {
        if (!isRunning)
            subscribeEvents();
        isRunning = true;
    }

    public void stop() {
        if (eventSubscription != null) {
            eventSubscription.unsubscribe();
            eventSubscription = null;
        }
    }

    public abstract Subscription subscribeEvents();
}
