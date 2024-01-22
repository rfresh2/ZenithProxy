package com.zenith.database;

import com.zenith.event.proxy.QueueCompleteEvent;
import com.zenith.event.proxy.QueuePositionUpdateEvent;
import com.zenith.event.proxy.ServerRestartingEvent;
import com.zenith.event.proxy.StartQueueEvent;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.EVENT_BUS;
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
    public void subscribeEvents() {
        EVENT_BUS.subscribe(this,
                            of(ServerRestartingEvent.class, this::handleServerRestart),
                            of(StartQueueEvent.class, this::handleStartQueue),
                            of(QueuePositionUpdateEvent.class, this::handleQueuePosition),
                            of(QueueCompleteEvent.class, this::handleQueueComplete)
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
            initialQueueLen = event.position();
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
        try (var handle = this.queryExecutor.getJdbi().open()) {
            handle.createUpdate("INSERT INTO queuewait (player_name, prio, initial_queue_len, start_queue_time, end_queue_time) VALUES (:player_name, :prio, :initial_queue_len, :start_queue_time, :end_queue_time)")
                    .bind("player_name", CONFIG.authentication.username)
                    .bind("prio", CONFIG.authentication.prio)
                    .bind("initial_queue_len", initialQueueLen)
                    .bind("start_queue_time", initialQueueTime.atOffset(ZoneOffset.UTC))
                    .bind("end_queue_time", endQueueTime.atOffset(ZoneOffset.UTC))
                    .execute();
        }
    }
}
