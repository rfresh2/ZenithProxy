package com.zenith.database;

import com.zenith.util.Wait;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jdbi.v3.core.HandleConsumer;
import org.jdbi.v3.core.Jdbi;

import java.util.function.Supplier;

import static com.zenith.Shared.DATABASE_LOG;

@Getter
@RequiredArgsConstructor
public class QueryExecutor {
    private final Jdbi jdbi;

    public void execute(final Supplier<HandleConsumer> queryProvider) {
        try (var handle = jdbi.open()) {
            queryProvider.get().useHandle(handle);
        } catch (final Exception e) {
            DATABASE_LOG.error("Failed executing query", e);
            Wait.waitMs(3000);
        }
    }
}
