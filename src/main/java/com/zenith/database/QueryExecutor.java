package com.zenith.database;

import com.zenith.util.Wait;
import org.jdbi.v3.core.HandleConsumer;
import org.jdbi.v3.core.Jdbi;

import java.util.function.Supplier;

import static com.zenith.Shared.DATABASE_LOG;

public record QueryExecutor(Jdbi jdbi) {
    public void execute(final Supplier<HandleConsumer> queryProvider) {
        try (var handle = jdbi.open()) {
            queryProvider.get().useHandle(handle);
        } catch (final Exception e) {
            DATABASE_LOG.error("Failed executing query", e);
            Wait.waitMs(3000);
        }
    }
}
