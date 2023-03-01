import com.zenith.database.ConnectionPool;
import com.zenith.database.dto.tables.Connections;
import com.zenith.database.dto.tables.Names;
import com.zenith.database.dto.tables.records.ConnectionsRecord;
import com.zenith.database.dto.tables.records.NamesRecord;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.zenith.util.Constants.DATABASE_LOG;

public class ConnectionsUuidCorrection {

    int batchCount = 0;

    //    @Test
    public void correctUuids() {
        // todo: find all null uuid's, lookup in names DB for matches
        final ConnectionPool inConnectionPool = new ConnectionPool("jdbc:postgresql://localhost:5432/postgres", "postgres", "a");
        final ConnectionPool outConnectionPool = new ConnectionPool();
        try (final Connection inConnection = inConnectionPool.getWriteConnection();
             final Connection outConnection = outConnectionPool.getWriteConnection()) {
            final DSLContext inContext = DSL.using(inConnection, SQLDialect.POSTGRES);
            final DSLContext outContext = DSL.using(outConnection, SQLDialect.POSTGRES);
            final Connections c = Connections.CONNECTIONS;
            final Result<ConnectionsRecord> connectionsRecords = outContext.selectFrom(c)
                    .where(c.PLAYER_UUID.isNull())
                    .fetch();
            DATABASE_LOG.info("Found {} connections records", connectionsRecords.size());
            final Names n = Names.NAMES;
            final List<Query> queryBuffer = new ArrayList<>();
            final int maxBuffer = 100;
            for (final ConnectionsRecord cr : connectionsRecords) {
                Result<NamesRecord> namesRecords = inContext.selectFrom(n)
                        .where(n.NAME.eq(cr.get(c.PLAYER_NAME))
                                .and(n.CHANGEDTOAT.isNull().or(n.CHANGEDTOAT.lt(cr.get(c.TIME)))))
                        .orderBy(n.CHANGEDTOAT.desc())
                        .limit(1)
                        .fetch();
                if (namesRecords.isEmpty()) {
                    continue;
                }
                final UUID playerUuid = namesRecords.get(0).get(n.UUID);
                UpdateConditionStep<ConnectionsRecord> query = outContext.update(c)
                        .set(c.PLAYER_UUID, playerUuid)
                        .where(c.TIME.eq(cr.get(c.TIME)).and(c.PLAYER_NAME.eq(cr.get(c.PLAYER_NAME))));
                queryBuffer.add(query);
                if (queryBuffer.size() > maxBuffer) {
                    writeBuffer(queryBuffer, outContext);
                    queryBuffer.clear();
                }
            }
            writeBuffer(queryBuffer, outContext);
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
