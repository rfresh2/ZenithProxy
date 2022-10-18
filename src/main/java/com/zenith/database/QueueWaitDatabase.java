package com.zenith.database;

import com.collarmc.pounce.Subscribe;
import com.zenith.Proxy;
import com.zenith.database.dto.tables.Queuewait;
import com.zenith.event.proxy.QueueCompleteEvent;
import com.zenith.event.proxy.StartQueueEvent;
import com.zenith.util.Queue;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static com.zenith.util.Constants.CONFIG;
import static com.zenith.util.Constants.DATABASE_LOG;

public class QueueWaitDatabase extends Database {

    private static final Duration MIN_QUEUE_DURATION = Duration.ofMinutes(3);
    private int initialQueueLen = 0;
    private Instant initialQueueTime = Instant.EPOCH;

    public QueueWaitDatabase(ConnectionPool connectionPool, Proxy proxy) {
        super(connectionPool, proxy);
    }

    @Subscribe
    public void handleStartQueue(final StartQueueEvent event) {
        // technically we could also get this from first queue update event
        initialQueueLen = CONFIG.authentication.prio ? Queue.getQueueStatus().prio : Queue.getQueueStatus().regular;
        initialQueueTime = Instant.now();
    }

    @Subscribe
    public void handleQueueComplete(final QueueCompleteEvent event) {
        Instant queueCompleteTime = Instant.now();
        // don't think there's much value in storing queue skips or immediate prio queues
        // will just need to filter them out in queries after
        // if there is a use case, just remove the condition
        if (queueCompleteTime.minus(MIN_QUEUE_DURATION).isAfter(initialQueueTime)) {
            writeQueueWait(initialQueueLen, initialQueueTime, queueCompleteTime);
        }
    }

    private void writeQueueWait(int initialQueueLen, Instant initialQueueTime, Instant endQueueTime) {
        try (final Connection connection = connectionPool.getWriteConnection()) {
            final DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);
            final Queuewait q = Queuewait.QUEUEWAIT;
            context.insertInto(q)
                    .set(q.PLAYER_NAME, CONFIG.authentication.username)
                    .set(q.PRIO, CONFIG.authentication.prio)
                    .set(q.INITIAL_QUEUE_LEN, initialQueueLen)
                    .set(q.START_QUEUE_TIME, initialQueueTime.atOffset(ZoneOffset.UTC)) // must be UTC
                    .set(q.END_QUEUE_TIME, endQueueTime.atOffset(ZoneOffset.UTC))
                    .execute();
        } catch (final SQLException e) {
            DATABASE_LOG.error("Error writing queue wait", e);
        }
    }
}
