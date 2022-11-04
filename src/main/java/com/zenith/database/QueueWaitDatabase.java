package com.zenith.database;

import com.collarmc.pounce.Subscribe;
import com.zenith.Proxy;
import com.zenith.database.dto.tables.Queuewait;
import com.zenith.event.proxy.QueueCompleteEvent;
import com.zenith.event.proxy.QueuePositionUpdateEvent;
import com.zenith.event.proxy.ServerRestartingEvent;
import com.zenith.event.proxy.StartQueueEvent;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.zenith.util.Constants.CONFIG;
import static com.zenith.util.Constants.DATABASE_LOG;
import static java.util.Objects.nonNull;

public class QueueWaitDatabase extends Database {

    private static final Duration MIN_RESTART_COOLDOWN = Duration.ofHours(6);
    private static final Duration MIN_QUEUE_DURATION = Duration.ofMinutes(3);
    private final AtomicBoolean shouldUpdateQueueLen = new AtomicBoolean(false);
    private Integer initialQueueLen = null;
    private Instant initialQueueTime = null;
    private Instant lastServerRestart = Instant.EPOCH;

    public QueueWaitDatabase(ConnectionPool connectionPool, Proxy proxy) {
        super(connectionPool, proxy);
    }

    @Subscribe
    public void handleServerRestart(final ServerRestartingEvent event) {
        lastServerRestart = Instant.now();
    }

    @Subscribe
    public void handleStartQueue(final StartQueueEvent event) {
        shouldUpdateQueueLen.set(true);
        initialQueueLen = null;
        initialQueueTime = null;
    }

    @Subscribe
    public void handleQueuePosition(final QueuePositionUpdateEvent event) {
        // record only first position update
        if (shouldUpdateQueueLen.compareAndSet(true, false)) {
            initialQueueLen = event.position;
            initialQueueTime = Instant.now();
        }
    }

    @Subscribe
    public void handleQueueComplete(final QueueCompleteEvent event) {
        final Instant queueCompleteTime = Instant.now();

        // filter out queue waits that happened directly after a server restart
        // these aren't very representative of normal queue waits
        // there might be a better way to filter these out
        if (!queueCompleteTime.minus(MIN_RESTART_COOLDOWN).isAfter(lastServerRestart)) {
            return;
        }

        // don't think there's much value in storing queue skips or immediate prio queues
        // will just need to filter them out in queries after
        // if there is a use case, just remove the condition
        if (nonNull(initialQueueTime) && nonNull(initialQueueLen)) {
            if (queueCompleteTime.minus(MIN_QUEUE_DURATION).isAfter(initialQueueTime)) {
                writeQueueWait(initialQueueLen, initialQueueTime, queueCompleteTime);
            }
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
