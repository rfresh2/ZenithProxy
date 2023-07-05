package com.zenith.database;

import static com.zenith.Shared.EVENT_BUS;

public abstract class Database {
    protected final QueryExecutor queryExecutor;

    public Database(final QueryExecutor queryExecutor) {
        this.queryExecutor = queryExecutor;
    }

    public void start() {
        EVENT_BUS.subscribe(this);
    }

    public void stop() {
        EVENT_BUS.unsubscribe(this);
    }
}
