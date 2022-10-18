package com.zenith.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;

public final class ConnectionPool {

    private final HikariDataSource writePool;
    private final HikariDataSource readPool;

    public ConnectionPool() {
        writePool = createDataSource(1);
        readPool = createDataSource(1);
    }

    public ConnectionPool(int size) {
        HikariDataSource dataSource = createDataSource(size);
        writePool = dataSource;
        readPool = dataSource;
    }

    private static String describePoolState(String name, HikariDataSource readPool) {
        HikariPoolMXBean bean = readPool.getHikariPoolMXBean();
        return "Acquiring connection from " + name + ". Waiting: " + bean.getThreadsAwaitingConnection() + " Active: " + bean.getActiveConnections() + " Idle: " + bean.getIdleConnections() + " Total: " + bean.getTotalConnections();
    }

    private static HikariDataSource createDataSource(int maxPoolSize) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");
        config.setJdbcUrl("jdbc:postgresql://mc-proxy.czyfkzmxjkdp.us-east-1.rds.amazonaws.com:5432/postgres");
        config.setUsername("proxy");
        config.setPassword("cghRVLQQiRqEpn9ccJEEeU");
        config.setMaximumPoolSize(maxPoolSize);
        config.setConnectionTimeout(5000);
        config.setKeepaliveTime(Duration.ofMinutes(1).toMillis());
        config.setMaxLifetime(Duration.ofMinutes(5).toMillis());
        return new HikariDataSource(config);
    }

    public Connection getWriteConnection() {
        try {
            return writePool.getConnection();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Connection getReadConnection() {
        try {
            Connection connection = readPool.getConnection();
            connection.setReadOnly(true);
            return connection;
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
