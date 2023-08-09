package com.zenith.database;

import com.zenith.database.dto.tables.Queuewait;
import com.zenith.database.dto.tables.records.QueuewaitRecord;
import com.zenith.event.Subscription;
import com.zenith.event.proxy.QueueCompleteEvent;
import com.zenith.event.proxy.QueuePositionUpdateEvent;
import com.zenith.event.proxy.ServerRestartingEvent;
import com.zenith.event.proxy.StartQueueEvent;
import org.jooq.DSLContext;
import org.jooq.InsertSetMoreStep;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.EVENT_BUS;
import static com.zenith.util.Pair.of;
import static java.util.Objects.nonNull;

public class QueueWaitDatabase extends Database {

    private static final Duration MIN_RESTART_COOLDOWN = Duration.ofHours(6);
    private static final Duration MIN_QUEUE_DURATION = Duration.ofMinutes(3);
    private final AtomicBoolean shouldUpdateQueueLen = new AtomicBoolean(false);
    private Integer initialQueueLen = null;
    private Instant initialQueueTime = null;
    private Instant lastServerRestart = Instant.EPOCH;

    public QueueWaitDatabase(QueryExecutor queryExecutor) {
        super(queryExecutor);
    }

    @Override
    public Subscription initEvents() {
        return EVENT_BUS.subscribe(
            of(ServerRestartingEvent.class, (Consumer<ServerRestartingEvent>)this::handleServerRestart),
            of(StartQueueEvent.class, (Consumer<StartQueueEvent>)this::handleStartQueue),
            of(QueuePositionUpdateEvent.class, (Consumer<QueuePositionUpdateEvent>)this::handleQueuePosition),
            of(QueueCompleteEvent.class, (Consumer<QueueCompleteEvent>)this::handleQueueComplete)
        );
    }

    public void handleServerRestart(final ServerRestartingEvent event) {
        lastServerRestart = Instant.now();
    }

    public void handleStartQueue(final StartQueueEvent event) {
        shouldUpdateQueueLen.set(true);
        initialQueueLen = null;
        initialQueueTime = null;
    }

    public void handleQueuePosition(final QueuePositionUpdateEvent event) {
        // record only first position update
        if (shouldUpdateQueueLen.compareAndSet(true, false)) {
            initialQueueLen = event.position;
            initialQueueTime = Instant.now();
        }
    }

    public void handleQueueComplete(final QueueCompleteEvent event) {
        final Instant queueCompleteTime = Instant.now();

        // filter out queue waits that happened directly after a server restart
        // these aren't very representative of normal queue waits
        // there might be a better way to filter these out
        // todo: problem: we often update the proxy right after a restart and won't have an accurate lastServerRestart value
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
        // todo: filter obvious undetected restart queues based on initial queue len and actual queue time
    }

    private void writeQueueWait(int initialQueueLen, Instant initialQueueTime, Instant endQueueTime) {
        final DSLContext context = DSL.using(SQLDialect.POSTGRES);
        final Queuewait q = Queuewait.QUEUEWAIT;
        InsertSetMoreStep<QueuewaitRecord> query = context.insertInto(q)
                .set(q.PLAYER_NAME, CONFIG.authentication.username)
                .set(q.PRIO, CONFIG.authentication.prio)
                .set(q.INITIAL_QUEUE_LEN, initialQueueLen)
                .set(q.START_QUEUE_TIME, initialQueueTime.atOffset(ZoneOffset.UTC)) // must be UTC
                .set(q.END_QUEUE_TIME, endQueueTime.atOffset(ZoneOffset.UTC));
        queryExecutor.execute(() -> query);
    }
}
