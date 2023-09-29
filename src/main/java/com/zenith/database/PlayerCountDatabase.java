package com.zenith.database;

import com.zenith.database.dto.tables.Playercount;
import com.zenith.database.dto.tables.records.PlayercountRecord;
import com.zenith.event.Subscription;
import com.zenith.event.proxy.DatabaseTickEvent;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static com.zenith.Shared.*;

public class PlayerCountDatabase extends LockingDatabase {

    public PlayerCountDatabase(QueryExecutor queryExecutor, RedisClient redisClient) {
        super(queryExecutor, redisClient);
    }

    @Override
    public Subscription subscribeEvents() {
        return EVENT_BUS.subscribe(
            DatabaseTickEvent.class, this::handleDatabaseTickEvent
        );
    }

    @Override
    public String getLockKey() {
        return "PlayerCount";
    }

    @Override
    public Instant getLastEntryTime() {
        final DSLContext context = DSL.using(SQLDialect.POSTGRES);
        Playercount p = Playercount.PLAYERCOUNT;
        Result<Record1<OffsetDateTime>> timeRecordResult = this.queryExecutor.fetch(context.select(p.TIME)
                .from(p)
                .orderBy(p.TIME.desc())
                .limit(1));
        if (timeRecordResult.isEmpty()) {
            DATABASE_LOG.warn("Player Count database unable to sync. Database empty?");
            return Instant.EPOCH;
        }
        return timeRecordResult.get(0).value1().toInstant();
    }

    public void handleDatabaseTickEvent(final DatabaseTickEvent event) {
        final int count = CACHE.getTabListCache().getTabList().getEntries().size();
        final DSLContext context = DSL.using(SQLDialect.POSTGRES);
        final Playercount p = Playercount.PLAYERCOUNT;
        final InsertSetMoreStep<PlayercountRecord> query = context.insertInto(p)
            .set(p.TIME, Instant.now().atOffset(ZoneOffset.UTC))
            .set(p.COUNT, (short) count);
        this.insert(Instant.now(), query);
    }
}
