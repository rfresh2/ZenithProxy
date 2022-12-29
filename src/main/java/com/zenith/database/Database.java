package com.zenith.database;

import static com.zenith.util.Constants.EVENT_BUS;

public abstract class Database {
    protected final ConnectionPool connectionPool;

    public Database(final ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    public void start() {
        EVENT_BUS.subscribe(this);
    }

    public void stop() {
        EVENT_BUS.unsubscribe(this);
    }
}
