package com.zenith.database;

import lombok.RequiredArgsConstructor;
import org.jdbi.v3.core.ConnectionFactory;
import org.jdbi.v3.core.statement.Cleanable;

import java.sql.Connection;
import java.sql.SQLException;

@RequiredArgsConstructor
public class HikariConnectionFactory implements ConnectionFactory {
    private final ConnectionPool connectionPool;
    @Override
    public Connection openConnection() throws SQLException {
        return connectionPool.getWriteConnection();
    }

    @Override
    public void closeConnection(final Connection conn) throws SQLException {
        ConnectionFactory.super.closeConnection(conn);
    }

    @Override
    public Cleanable getCleanableFor(final Connection conn) {
        return ConnectionFactory.super.getCleanableFor(conn);
    }
}
