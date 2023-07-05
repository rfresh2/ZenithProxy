package com.zenith.database;

import com.collarmc.pounce.Subscribe;
import com.zenith.database.dto.tables.Queuelength;
import com.zenith.database.dto.tables.records.QueuelengthRecord;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.feature.queue.Queue;
import com.zenith.feature.queue.QueueStatus;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.DATABASE_LOG;

public class QueueLengthDatabase extends LockingDatabase {
    private static final Duration updateInterval = Duration.ofMinutes(CONFIG.server.queueStatusRefreshMinutes + 1L);
    private Instant lastUpdate = Instant.EPOCH;

    public QueueLengthDatabase(QueryExecutor queryExecutor, RedisClient redisClient) {
        super(queryExecutor, redisClient);
    }

    @Override
    public String getLockKey() {
        return "QueueLength";
    }

    @Override
    public Instant getLastEntryTime() {
        final DSLContext context = DSL.using(SQLDialect.POSTGRES);
        final Queuelength q = Queuelength.QUEUELENGTH;
        Result<Record1<OffsetDateTime>> timeRecordResult = this.queryExecutor.fetch(context.select(q.TIME)
                .from(q)
                .orderBy(q.TIME.desc())
                .limit(1));
        if (timeRecordResult.isEmpty()) {
            DATABASE_LOG.warn("QueueLength database unable to sync. Database empty?");
            return Instant.EPOCH;
        }
        return timeRecordResult.get(0).value1().toInstant();
    }

    @Subscribe
    public void handleTickEvent(final ClientTickEvent event) {
        if (lastUpdate.isBefore(Instant.now().minus(updateInterval))) {
            lastUpdate = Instant.now();
            final QueueStatus queueStatus = Queue.getQueueStatus();
            final DSLContext context = DSL.using(SQLDialect.POSTGRES);
            final Queuelength q = Queuelength.QUEUELENGTH;
            final InsertSetMoreStep<QueuelengthRecord> query = context.insertInto(q)
                    .set(q.TIME, Instant.now().atOffset(ZoneOffset.UTC))
                    .set(q.REGULAR, queueStatus.regular.shortValue())
                    .set(q.PRIO, queueStatus.prio.shortValue());
            this.insert(Instant.now(), query);
        }
    }
}
