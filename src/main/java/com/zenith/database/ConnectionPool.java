package com.zenith.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.time.Duration;

import static com.zenith.Shared.CONFIG;

public final class ConnectionPool {

    private final HikariDataSource writePool;

    public ConnectionPool() {
        writePool = createDataSource();
    }

    private static HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");
        config.setJdbcUrl("jdbc:postgresql://" + CONFIG.database.host + ":" + CONFIG.database.port + "/postgres");
        config.setUsername(CONFIG.database.username);
        config.setPassword(CONFIG.database.password);
        config.setMaximumPoolSize(1);
        config.setConnectionTimeout(5000);
        config.addDataSourceProperty("loginTimeout", 60);
        config.addDataSourceProperty("tcpKeepAlive", true);
        config.addDataSourceProperty("socketTimeout", 60);
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
