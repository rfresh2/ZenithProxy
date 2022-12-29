package com.zenith.database;

import lombok.Getter;

import static com.zenith.util.Constants.CONFIG;
import static java.util.Objects.nonNull;

@Getter
public class DatabaseManager {
    private QueueWaitDatabase queueWaitDatabase;
    private ConnectionsDatabase connectionsDatabase;
    private ConnectionPool connectionPool;

    public DatabaseManager() {
        if (CONFIG.database.queueWait.enabled) {
            startQueueWaitDatabase();
        }
        if (CONFIG.database.connections.testingEnabled) {
            startConnectionsDatabase();
        }
    }

    public void startQueueWaitDatabase() {
        if (nonNull(this.queueWaitDatabase)) {
            this.queueWaitDatabase.start();
        } else {
            this.queueWaitDatabase = new QueueWaitDatabase(getConnectionPool());
            this.queueWaitDatabase.start();
        }
    }

    public void stopQueueWaitDatabase() {
        if (nonNull(this.queueWaitDatabase)) {
            this.queueWaitDatabase.stop();
        }
    }

    public void startConnectionsDatabase() {
        if (nonNull(this.connectionsDatabase)) {
            this.connectionsDatabase.start();
        } else {
            this.connectionsDatabase = new ConnectionsDatabase(getConnectionPool());
            this.connectionsDatabase.start();
        }
    }

    public void stopConnectionsDatabase() {
        if (nonNull(this.connectionsDatabase)) {
            this.connectionsDatabase.stop();
        }
    }

    private synchronized ConnectionPool getConnectionPool() {
        if (nonNull(this.connectionPool)) {
            return connectionPool;
        } else {
            this.connectionPool = new ConnectionPool();
            return this.connectionPool;
        }
    }
}
