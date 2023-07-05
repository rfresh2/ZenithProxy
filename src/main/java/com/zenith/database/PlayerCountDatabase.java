package com.zenith.database;

import com.collarmc.pounce.Subscribe;
import com.zenith.Proxy;
import com.zenith.database.dto.tables.Playercount;
import com.zenith.database.dto.tables.records.PlayercountRecord;
import com.zenith.event.module.ClientTickEvent;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.DATABASE_LOG;

public class PlayerCountDatabase extends LockingDatabase {
    private static final Duration updateInterval = Duration.ofMinutes(5L);
    private Instant lastUpdate = Instant.EPOCH;

    public PlayerCountDatabase(QueryExecutor queryExecutor, RedisClient redisClient) {
        super(queryExecutor, redisClient);
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

    @Subscribe
    public void handleClientTickEvent(final ClientTickEvent event) {
        if (lastUpdate.isBefore(Instant.now().minus(updateInterval))) {
            if (!Proxy.getInstance().isOnlineOn2b2tForAtLeastDuration(Duration.ofSeconds(30L))) return;
            lastUpdate = Instant.now();
            final Integer count = CACHE.getTabListCache().getTabList().getEntries().size();
            final DSLContext context = DSL.using(SQLDialect.POSTGRES);
            final Playercount p = Playercount.PLAYERCOUNT;
            final InsertSetMoreStep<PlayercountRecord> query = context.insertInto(p)
                    .set(p.TIME, Instant.now().atOffset(ZoneOffset.UTC))
                    .set(p.COUNT, count.shortValue());
            this.insert(Instant.now(), query);
        }
    }
}
