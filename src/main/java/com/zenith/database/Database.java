package com.zenith.database;

import com.zenith.Proxy;

import static com.zenith.util.Constants.EVENT_BUS;

public abstract class Database {
    protected final ConnectionPool connectionPool;
    protected final Proxy proxy;

    public Database(final ConnectionPool connectionPool, final Proxy proxy) {
        this.connectionPool = connectionPool;
        this.proxy = proxy;
        EVENT_BUS.subscribe(this);
    }
}
