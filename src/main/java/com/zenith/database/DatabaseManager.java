package com.zenith.database;

import lombok.Getter;

import static com.zenith.util.Constants.CONFIG;
import static java.util.Objects.nonNull;

@Getter
public class DatabaseManager {
    private QueueWaitDatabase queueWaitDatabase;
    private ConnectionsDatabase connectionsDatabase;
    private ChatDatabase chatDatabase;
    private DeathsDatabase deathsDatabase;
    private ConnectionPool connectionPool;
    private QueryQueue queryQueue;

    public DatabaseManager() {
        queryQueue = new QueryQueue(this::getConnectionPool);
        queryQueue.start();
        if (CONFIG.database.queueWait.enabled) {
            startQueueWaitDatabase();
        }
        if (CONFIG.database.connections.enabled) {
            startConnectionsDatabase();
        }
        if (CONFIG.database.chats.enabled) {
            startChatsDatabase();
        }
        if (CONFIG.database.deaths.enabled) {
            startDeathsDatabase();
        }
    }

    public void startQueueWaitDatabase() {
        if (nonNull(this.queueWaitDatabase)) {
            this.queueWaitDatabase.start();
        } else {
            this.queueWaitDatabase = new QueueWaitDatabase(queryQueue);
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
            this.connectionsDatabase = new ConnectionsDatabase(queryQueue);
            this.connectionsDatabase.start();
        }
    }

    public void stopConnectionsDatabase() {
        if (nonNull(this.connectionsDatabase)) {
            this.connectionsDatabase.stop();
        }
    }

    public void startChatsDatabase() {
        if (nonNull(this.chatDatabase)) {
            this.chatDatabase.start();
        } else {
            this.chatDatabase = new ChatDatabase(queryQueue);
            this.chatDatabase.start();
        }
    }

    public void stopChatsDatabase() {
        if (nonNull(this.chatDatabase)) {
            this.chatDatabase.stop();
        }
    }

    public void startDeathsDatabase() {
        if (nonNull(this.deathsDatabase)) {
            this.deathsDatabase.start();
        } else {
            this.deathsDatabase = new DeathsDatabase(queryQueue);
            this.deathsDatabase.start();
        }
    }

    public void stopDeathsDatabase() {
        if (nonNull(this.deathsDatabase)) {
            this.deathsDatabase.stop();
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
