package com.zenith.database;

import com.zenith.util.Wait;
import org.jooq.Record;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.util.function.Supplier;

import static com.zenith.Shared.DATABASE_LOG;

public class QueryExecutor {
    private final Supplier<ConnectionPool> connectionPoolProvider;

    public QueryExecutor(final Supplier<ConnectionPool> connectionPoolProvider) {
        this.connectionPoolProvider = connectionPoolProvider;
    }

    public void execute(final Supplier<Query> queryProvider) {
        try (final Connection connection = connectionPoolProvider.get().getWriteConnection()) {
            final DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);
            context.execute(queryProvider.get());
        } catch (final Exception e) {
            DATABASE_LOG.error("Failed executing query", e);
            Wait.waitALittleMs(3000);
        }
    }

    public <R extends Record> Result<R> fetch(final Select<R> select) {
        try (final Connection connection = connectionPoolProvider.get().getWriteConnection()) {
            final DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);
            return context.fetch(select);
        } catch (final Exception e) {
            DATABASE_LOG.error("Failed selecting from database: {}", select, e);
            throw new RuntimeException(e);
        }
    }
}
