package com.zenith.database;

import static com.zenith.Globals.EVENT_BUS;

public abstract class Database {
    protected final QueryExecutor queryExecutor;
    boolean isRunning = false;

    public Database(final QueryExecutor queryExecutor) {
        this.queryExecutor = queryExecutor;
    }

    public void start() {
        subscribeEvents();
        isRunning = true;
    }

    public void stop() {
        EVENT_BUS.unsubscribe(this);
        isRunning = false;
    }

    public abstract void subscribeEvents();
}
