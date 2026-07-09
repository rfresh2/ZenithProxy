package com.zenith.database;

import com.zenith.Proxy;
import com.zenith.event.db.DatabaseTickEvent;
import lombok.Getter;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.zenith.Globals.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Getter
public class DatabaseManager {
    private QueueWaitDatabase queueWaitDatabase;
    private ConnectionsDatabase connectionsDatabase;
    private ChatDatabase chatDatabase;
    private DeathsDatabase deathsDatabase;
    private Jdbi jdbi;
    private HikariConnectionFactory connectionFactory;
    private QueueLengthDatabase queueLengthDatabase;
    private RestartsDatabase restartsDatabase;
    private PlayerCountDatabase playerCountDatabase;
    private TablistDatabase tablistDatabase;
    private PlaytimeDatabase playtimeDatabase;
    private TimeDatabase timeDatabase;
    private QueryExecutor queryExecutor;
    private RedisClient redisClient;
    private ScheduledFuture<?> databaseTickFuture;

    public synchronized void start() {
        try {
            this.queryExecutor = new QueryExecutor(getJdbi());
            if (CONFIG.database.queueWaitEnabled) {
                startQueueWaitDatabase();
            }
            if (CONFIG.database.connectionsEnabled) {
                startConnectionsDatabase();
            }
            if (CONFIG.database.chatsEnabled) {
                startChatsDatabase();
            }
            if (CONFIG.database.deathsEnabled) {
                startDeathsDatabase();
            }
            if (CONFIG.database.queueLengthEnabled) {
                startQueueLengthDatabase();
            }
            if (CONFIG.database.restartsEnabled) {
                startRestartsDatabase();
            }
            if (CONFIG.database.playerCountEnabled) {
                startPlayerCountDatabase();
            }
            if (CONFIG.database.tablistEnabled) {
                startTablistDatabase();
            }
            if (CONFIG.database.playtimeEnabled) {
                startPlaytimeDatabase();
            }
            if (CONFIG.database.timeEnabled) {
                startTimeDatabase();
            }
            if (databaseTickFuture != null) {
                databaseTickFuture.cancel(false);
            }
            databaseTickFuture = EXECUTOR.scheduleWithFixedDelay(
                this::postDatabaseTick,
                DatabaseTickEvent.TICK_INTERVAL_SECONDS,
                DatabaseTickEvent.TICK_INTERVAL_SECONDS,
                TimeUnit.SECONDS
            );
        } catch (final Exception e) {
            DATABASE_LOG.error("Failed starting databases", e);
        }
    }

    public synchronized void stop() {
        try {
            if (nonNull(databaseTickFuture)) databaseTickFuture.cancel(false);
            stopQueueWaitDatabase();
            stopConnectionsDatabase();
            stopChatsDatabase();
            stopDeathsDatabase();
            stopQueueLengthDatabase();
            stopRestartsDatabase();
            stopPlayerCountDatabase();
            stopTablistDatabase();
            stopPlaytimeDatabase();
            stopRedisClient();
            stopJdbi();
        } catch (final Exception e) {
            DATABASE_LOG.error("Failed stopping databases", e);
        }
    }

    public void postDatabaseTick() {
        try {
            // todo: there's a (uncommon) race condition here, if the proxy disconnects and the cache resets during the tick event
            //  the faster each handler executes the more unlikely that is to happen but its still possible
            if (Proxy.getInstance().isOnlineOn2b2tForAtLeastDuration(Duration.ofSeconds(30)))
                EVENT_BUS.post(DatabaseTickEvent.INSTANCE);
        } catch (final Throwable e) {
            DATABASE_LOG.error("Failed posting database tick event", e);
        }
    }

    public void startQueueWaitDatabase() {
        if (isNull(queueWaitDatabase)) {
            this.queueWaitDatabase = new QueueWaitDatabase(queryExecutor);
            this.queueWaitDatabase.start();
        }
    }

    public void stopQueueWaitDatabase() {
        if (nonNull(this.queueWaitDatabase)) {
            this.queueWaitDatabase.stop();
            this.queueWaitDatabase = null;
        }
    }

    public void startConnectionsDatabase() {
        if (isNull(connectionsDatabase)) {
            this.connectionsDatabase = new ConnectionsDatabase(queryExecutor, getRedisClient());
            this.connectionsDatabase.start();
        }
    }

    public void stopConnectionsDatabase() {
        if (nonNull(this.connectionsDatabase)) {
            this.connectionsDatabase.stop();
            this.connectionsDatabase = null;
        }
    }

    public void startChatsDatabase() {
        if (isNull(chatDatabase)) {
            this.chatDatabase = new ChatDatabase(queryExecutor, getRedisClient());
            this.chatDatabase.start();
        }
    }

    public void stopChatsDatabase() {
        if (nonNull(this.chatDatabase)) {
            this.chatDatabase.stop();
            this.chatDatabase = null;
        }
    }

    public void startDeathsDatabase() {
        if (isNull(deathsDatabase)) {
            this.deathsDatabase = new DeathsDatabase(queryExecutor, getRedisClient());
            this.deathsDatabase.start();
        }
    }

    public void stopDeathsDatabase() {
        if (nonNull(this.deathsDatabase)) {
            this.deathsDatabase.stop();
            this.deathsDatabase = null;
        }
    }

    public void startQueueLengthDatabase() {
        if (isNull(this.queueLengthDatabase)) {
            this.queueLengthDatabase = new QueueLengthDatabase(queryExecutor, getRedisClient());
            this.queueLengthDatabase.start();
        }
    }

    public void stopQueueLengthDatabase() {
        if (nonNull(this.queueLengthDatabase)) {
            this.queueLengthDatabase.stop();
            this.queueLengthDatabase = null;
        }
    }

    public void startRestartsDatabase() {
        if (isNull(restartsDatabase)) {
            this.restartsDatabase = new RestartsDatabase(queryExecutor, getRedisClient());
            this.restartsDatabase.start();
        }
    }

    public void stopRestartsDatabase() {
        if (nonNull(this.restartsDatabase)) {
            this.restartsDatabase.stop();
            this.restartsDatabase = null;
        }
    }

    public void startPlayerCountDatabase() {
        if (isNull(playerCountDatabase)) {
            this.playerCountDatabase = new PlayerCountDatabase(queryExecutor, getRedisClient());
            this.playerCountDatabase.start();
        }
    }

    public void stopPlayerCountDatabase() {
        if (nonNull(this.playerCountDatabase)) {
            this.playerCountDatabase.stop();
            this.playerCountDatabase = null;
        }
    }

    public void startTablistDatabase() {
        if (isNull(tablistDatabase)) {
            this.tablistDatabase = new TablistDatabase(queryExecutor, getRedisClient());
            this.tablistDatabase.start();
        }
    }

    public void stopTablistDatabase() {
        if (nonNull(this.tablistDatabase)) {
            this.tablistDatabase.stop();
            this.tablistDatabase = null;
        }
    }

    public void startPlaytimeDatabase() {
        if (isNull(playtimeDatabase)) {
            this.playtimeDatabase = new PlaytimeDatabase(queryExecutor, getRedisClient());
            this.playtimeDatabase.start();
        }
    }

    public void stopPlaytimeDatabase() {
        if (nonNull(this.playtimeDatabase)) {
            this.playtimeDatabase.stop();
            this.playtimeDatabase = null;
        }
    }

    public void startTimeDatabase() {
        if (isNull(timeDatabase)) {
            this.timeDatabase = new TimeDatabase(queryExecutor, getRedisClient());
            this.timeDatabase.start();
        }
    }

    public void stopTimeDatabase() {
        if (nonNull(this.timeDatabase)) {
            this.timeDatabase.stop();
            this.timeDatabase = null;
        }
    }

    private HikariConnectionFactory getConnectionFactory() {
        if (isNull(this.connectionFactory)) {
            this.connectionFactory = new HikariConnectionFactory(new ConnectionPool());
        }
        return connectionFactory;
    }

    private synchronized Jdbi getJdbi() {
        if (isNull(this.jdbi)) {
            this.jdbi = Jdbi.create(getConnectionFactory());
            this.jdbi.installPlugin(new PostgresPlugin());
        }
        return jdbi;
    }

    private void stopJdbi() {
        if (jdbi != null) {
            jdbi = null;
        }
        if (connectionFactory != null) {
            connectionFactory.getConnectionPool().close();
            connectionFactory = null;
        }
    }

    private synchronized RedisClient getRedisClient() {
        if (isNull(this.redisClient)) this.redisClient = new RedisClient();
        return redisClient;
    }

    private void stopRedisClient() {
        if (nonNull(this.redisClient)) {
            this.redisClient.getRedissonClient().shutdown();
            this.redisClient = null;
        }
    }
}
