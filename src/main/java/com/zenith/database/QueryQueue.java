package com.zenith.database;

import com.zenith.util.Wait;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.zenith.util.Constants.DATABASE_LOG;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class QueryQueue {
    private final ConcurrentLinkedQueue<Query> queue = new ConcurrentLinkedQueue<>();
    private final Random random = new Random();
    private final Supplier<ConnectionPool> connectionPoolProvider;
    private ScheduledExecutorService queryExecutorPool;

    public QueryQueue(final Supplier<ConnectionPool> connectionPoolProvider) {
        this.connectionPoolProvider = connectionPoolProvider;
        this.queryExecutorPool = null;
    }

    public synchronized void start() {
        if (isNull(queryExecutorPool)) {
            queryExecutorPool = Executors.newSingleThreadScheduledExecutor();
            queryExecutorPool.scheduleWithFixedDelay(this::processQueue, 0L, 100, TimeUnit.MILLISECONDS);
        }
    }

    public synchronized void stop() {
        queryExecutorPool.shutdownNow();
        queryExecutorPool = null;
    }

    public boolean isRunning() {
        return isNull(queryExecutorPool) || !queryExecutorPool.isShutdown();
    }

    public void add(final Query q) {
        queue.add(q);
    }

    private void processQueue() {
        final Query query = queue.poll();
        if (nonNull(query)) {
            try (final Connection connection = connectionPoolProvider.get().getWriteConnection()) {
                final DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);
                context.execute(query);
            } catch (final Exception e) {
                if (e.getMessage().contains("violates exclusion constraint") || e.getMessage().contains("deadlock detected")) {
                    // expected due to multiple proxies writing the same events
                } else {
                    DATABASE_LOG.error("Failed executing query: {}", query, e);
                    Wait.waitALittleMs(3000);
                }
            }
        }
        Wait.waitALittleMs(random.nextInt(100));
    }
}
