package com.zenith.database;

import com.zenith.database.dto.tables.Tablist;
import com.zenith.database.dto.tables.records.TablistRecord;
import com.zenith.event.Subscription;
import com.zenith.event.proxy.DatabaseTickEvent;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static com.zenith.Shared.*;

public class TablistDatabase extends LockingDatabase {
    public TablistDatabase(final QueryExecutor queryExecutor, final RedisClient redisClient) {
        super(queryExecutor, redisClient);
    }

    @Override
    public Subscription subscribeEvents() {
        return EVENT_BUS.subscribe(
            DatabaseTickEvent.class, this::handleTickEvent
        );
    }

    private void handleTickEvent(DatabaseTickEvent event) {
        this.queryExecutor.executeTransaction(this::syncTablist);
    }

    private void syncTablist(Configuration configuration) {
        final Tablist t = Tablist.TABLIST;
        final DSLContext ctx = configuration.dsl();
        ctx.deleteFrom(t).execute();
        ctx.batchInsert(CACHE.getTabListCache().getTabList().getEntries().stream()
                            .map(e -> new TablistRecord(e.getName(),
                                                        e.getId(),
                                                        Instant.now().atOffset(ZoneOffset.UTC)))
                            .toList())
            .execute();
    }

    private void correctConnections(Configuration configuration) {
        // todo: find connect/disconnect events we missed based on the current tablist
        //  then insert them into the connections database
        //  we need to add some delay on this for new events to come in
        //  we also need to avoid correcting events older than some range
    }

    @Override
    public String getLockKey() {
        return "Tablist";
    }

    @Override
    public Instant getLastEntryTime() {
        final DSLContext context = DSL.using(SQLDialect.POSTGRES);
        final Tablist t = Tablist.TABLIST;
        Result<Record1<OffsetDateTime>> result = this.queryExecutor.fetch(context.select(t.TIME)
                                                                             .from(t)
                                                                             .orderBy(t.TIME.desc())
                                                                             .limit(1));
        if (result.isEmpty()) {
            DATABASE_LOG.warn("Tablist database unable to sync. Database empty?");
            return Instant.EPOCH;
        }
        return result.get(0).value1().toInstant();
    }
}
