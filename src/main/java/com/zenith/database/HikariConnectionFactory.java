package com.zenith.database;

import lombok.RequiredArgsConstructor;
import org.jdbi.v3.core.ConnectionFactory;

import java.sql.Connection;

@RequiredArgsConstructor
public class HikariConnectionFactory implements ConnectionFactory {
    private final ConnectionPool connectionPool;

    @Override
    public Connection openConnection() {
        return connectionPool.getWriteConnection();
    }
}
