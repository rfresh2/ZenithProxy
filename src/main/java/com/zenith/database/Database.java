package com.zenith.database;

import static com.zenith.util.Constants.EVENT_BUS;

public abstract class Database {
    protected final QueryQueue queryQueue;

    public Database(final QueryQueue queryQueue) {
        this.queryQueue = queryQueue;
    }

    public void start() {
        EVENT_BUS.subscribe(this);
    }

    public void stop() {
        EVENT_BUS.unsubscribe(this);
    }
}
