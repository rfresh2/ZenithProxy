package com.zenith.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.time.Duration;

import static com.zenith.Shared.CONFIG;

public final class ConnectionPool {

    private final HikariDataSource writePool;

    public ConnectionPool() {
        writePool = createDataSource(CONFIG.database.writePool);
    }

    public ConnectionPool(final String customUrl, final String customUser, final String customPass) {
        writePool = createDataSource(1, customUrl, customUser, customPass);
    }

    private static HikariDataSource createDataSource(int maxPoolSize, String url, String user, String pass) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(pass);
        config.setMaximumPoolSize(maxPoolSize);
        config.setConnectionTimeout(5000);
        config.setKeepaliveTime(Duration.ofMinutes(1).toMillis());
        config.setMaxLifetime(Duration.ofMinutes(5).toMillis());
        return new HikariDataSource(config);
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
}
