package com.zenith.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.time.Duration;

import static com.zenith.util.Constants.CONFIG;
import static com.zenith.util.Constants.DATABASE_LOG;

public final class ConnectionPool {

    private final HikariDataSource writePool;
    private final HikariDataSource readPool;

    public ConnectionPool() {
        HikariDataSource wPool = null;
        try {
            if (CONFIG.database.writePool > 0) {
                wPool = createDataSource(CONFIG.database.writePool);
            }
        } catch (final Exception e) {
            DATABASE_LOG.error("Error initializing database write connection pool", e);
        }
        writePool = wPool;
        HikariDataSource rPool = null;
        try {
            if (CONFIG.database.readPool > 0) {
                rPool = createDataSource(CONFIG.database.readPool);
            }
        } catch (final Exception e) {
            DATABASE_LOG.error("Error initializing database read connection pool", e);
        }
        readPool = rPool;
    }

    private static HikariDataSource createDataSource(int maxPoolSize) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");
        config.setJdbcUrl("jdbc:postgresql://" + CONFIG.database.host + ":" + CONFIG.database.port + "/postgres");
        config.setUsername(CONFIG.database.username);
        config.setPassword(CONFIG.database.password);
        config.setMaximumPoolSize(maxPoolSize);
        config.setConnectionTimeout(5000);
        config.setKeepaliveTime(Duration.ofMinutes(1).toMillis());
        config.setMaxLifetime(Duration.ofMinutes(5).toMillis());
        return new HikariDataSource(config);
    }

    public Connection getWriteConnection() {
        try {
            return writePool.getConnection();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Connection getReadConnection() {
        try {
            Connection connection = readPool.getConnection();
            connection.setReadOnly(true);
            return connection;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
