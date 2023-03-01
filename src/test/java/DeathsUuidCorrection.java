import com.zenith.database.ConnectionPool;
import com.zenith.database.dto.tables.Deaths;
import com.zenith.database.dto.tables.Names;
import com.zenith.database.dto.tables.records.DeathsRecord;
import com.zenith.database.dto.tables.records.NamesRecord;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.zenith.util.Constants.DATABASE_LOG;

public class DeathsUuidCorrection {
    int batchCount = 0;

    //    @Test
    public void correctUuids() {
        // todo: find all null uuid's, lookup in names DB for matches
        final ConnectionPool inConnectionPool = new ConnectionPool("jdbc:postgresql://localhost:5432/postgres", "postgres", "a");
        try (final Connection inConnection = inConnectionPool.getWriteConnection()) {
            final DSLContext inContext = DSL.using(inConnection, SQLDialect.POSTGRES);
            final Deaths d = Deaths.DEATHS;
            final Result<DeathsRecord> deathsRecords = inContext.selectFrom(d)
                    .where(d.VICTIM_PLAYER_UUID.isNull())
                    .fetch();
            DATABASE_LOG.info("Found {} deaths records", deathsRecords.size());
            final Names n = Names.NAMES;
            final List<Query> queryBuffer = new ArrayList<>();
            final int maxBuffer = 100;
            for (final DeathsRecord dr : deathsRecords) {
                try {
                    Result<NamesRecord> namesRecords = inContext.selectFrom(n)
                            .where(n.NAME.eq(dr.get(d.VICTIM_PLAYER_NAME))
                                    .and(n.CHANGEDTOAT.isNull().or(n.CHANGEDTOAT.lt(dr.get(d.TIME)))))
                            .orderBy(n.CHANGEDTOAT.desc())
                            .limit(1)
                            .fetch();
                    if (namesRecords.isEmpty()) {
                        continue;
                    }
                    final UUID playerUuid = namesRecords.get(0).getUuid();
                    UpdateConditionStep<DeathsRecord> query = inContext.update(d)
                            .set(d.VICTIM_PLAYER_UUID, playerUuid)
                            .where(d.TIME.eq(dr.get(d.TIME)).and(d.VICTIM_PLAYER_NAME.eq(dr.get(d.VICTIM_PLAYER_NAME))));
                    queryBuffer.add(query);
                    if (queryBuffer.size() > maxBuffer) {
                        writeBuffer(queryBuffer, inContext);
                        queryBuffer.clear();
                    }
                } catch (final Exception e) {
                    DATABASE_LOG.error("", e);
                }

            }
            writeBuffer(queryBuffer, inContext);
            queryBuffer.clear();

        } catch (final Exception e) {
            DATABASE_LOG.error("", e);
        }
    }

    private void writeBuffer(List<Query> buffer, DSLContext inContext) {
        DATABASE_LOG.info("Writing batch {}", batchCount++);
        inContext.batch(buffer).execute();
    }
}
