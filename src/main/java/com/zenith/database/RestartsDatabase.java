package com.zenith.database;

import com.collarmc.pounce.Subscribe;
import com.zenith.database.dto.tables.Restarts;
import com.zenith.database.dto.tables.records.RestartsRecord;
import com.zenith.event.proxy.ServerRestartingEvent;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static com.zenith.Shared.DATABASE_LOG;

public class RestartsDatabase extends LockingDatabase {

    private static final Duration cooldownDuration = Duration.ofMinutes(20L);
    private Instant lastRestartWrite = Instant.EPOCH;

    public RestartsDatabase(QueryExecutor queryExecutor, RedisClient redisClient) {
        super(queryExecutor, redisClient);
    }

    @Override
    public String getLockKey() {
        return "Restarts";
    }

    @Override
    public Instant getLastEntryTime() {
        final DSLContext context = DSL.using(SQLDialect.POSTGRES);
        final Restarts r = Restarts.RESTARTS;
        final Result<Record1<OffsetDateTime>> timeRecordResult = this.queryExecutor.fetch(context.select(r.TIME)
                .from(r)
                .orderBy(r.TIME.desc())
                .limit(1));
        if (timeRecordResult.isEmpty()) {
            DATABASE_LOG.warn("Restarts database unable to sync. Database empty?");
            return Instant.EPOCH;
        }
        return timeRecordResult.get(0).value1().toInstant();
    }

    @Subscribe
    public void handleServerRestartEvent(final ServerRestartingEvent event) {
        synchronized (this) {
            if (lastRestartWrite.isBefore(Instant.now().minus(cooldownDuration))) {
                lastRestartWrite = Instant.now();
                final DSLContext context = DSL.using(SQLDialect.POSTGRES);
                final Restarts r = Restarts.RESTARTS;
                final InsertSetMoreStep<RestartsRecord> query = context.insertInto(r)
                        .set(r.TIME, Instant.now().plus(Duration.ofMinutes(15L)).atOffset(ZoneOffset.UTC));
                this.insert(Instant.now(), query);
            }
        }
    }
}
