package com.zenith.database;

import static com.zenith.Shared.EVENT_BUS;

public abstract class Database {
    protected final QueryExecutor queryExecutor;
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
        EVENT_BUS.unsubscribe(this);
    }

    public abstract void subscribeEvents();
}
