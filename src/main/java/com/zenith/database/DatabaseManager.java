package com.zenith.database;

import lombok.Getter;

import static com.zenith.util.Constants.CONFIG;
import static com.zenith.util.Constants.DATABASE_LOG;
import static java.util.Objects.nonNull;

@Getter
public class DatabaseManager {
    private QueueWaitDatabase queueWaitDatabase;
    private ConnectionsDatabase connectionsDatabase;
    private ChatDatabase chatDatabase;
    private DeathsDatabase deathsDatabase;
    private ConnectionPool connectionPool;
    private QueueLengthDatabase queueLengthDatabase;
    private RestartsDatabase restartsDatabase;
    private QueryExecutor queryExecutor;

    public DatabaseManager() {
        try {
            this.queryExecutor = new QueryExecutor(this::getConnectionPool);
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
            if (CONFIG.database.queueLength.enabled) {
                startQueueLengthDatabase();
            }
            if (CONFIG.database.restarts.enabled) {
                startRestartsDatabase();
            }
        } catch (final Exception e) {
            DATABASE_LOG.error("Failed starting databases", e);
        }
    }

    public void startQueueWaitDatabase() {
        if (nonNull(this.queueWaitDatabase)) {
            this.queueWaitDatabase.start();
        } else {
            this.queueWaitDatabase = new QueueWaitDatabase(queryExecutor);
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
            this.connectionsDatabase = new ConnectionsDatabase(queryExecutor, new RedisClient());
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
            this.chatDatabase = new ChatDatabase(queryExecutor, new RedisClient());
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
            this.deathsDatabase = new DeathsDatabase(queryExecutor, new RedisClient());
            this.deathsDatabase.start();
        }
    }

    public void stopDeathsDatabase() {
        if (nonNull(this.deathsDatabase)) {
            this.deathsDatabase.stop();
        }
    }

    public void startQueueLengthDatabase() {
        if (nonNull(this.queueLengthDatabase)) {
            this.queueLengthDatabase.start();
        } else {
            this.queueLengthDatabase = new QueueLengthDatabase(queryExecutor, new RedisClient());
            this.queueLengthDatabase.start();
        }
    }

    public void stopQueueLengthDatabase() {
        if (nonNull(this.queueLengthDatabase)) {
            this.queueLengthDatabase.stop();
        }
    }

    public void startRestartsDatabase() {
        if (nonNull(this.restartsDatabase)) {
            this.restartsDatabase.start();
        } else {
            this.restartsDatabase = new RestartsDatabase(queryExecutor, new RedisClient());
            this.restartsDatabase.start();
        }
    }

    public void stopRestartsDatabase() {
        if (nonNull(this.restartsDatabase)) {
            this.restartsDatabase.stop();
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
