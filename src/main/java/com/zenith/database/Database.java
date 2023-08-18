package com.zenith.database;

import com.zenith.event.Subscription;

public abstract class Database {
    protected final QueryExecutor queryExecutor;
    private Subscription eventSubscription;

    public Database(final QueryExecutor queryExecutor) {
        this.queryExecutor = queryExecutor;
    }

    public void start() {
        subscribeEvents();
    }

    public void stop() {
        if (eventSubscription != null) {
            eventSubscription.unsubscribe();
            eventSubscription = null;
        }
    }

    public abstract Subscription subscribeEvents();
}
