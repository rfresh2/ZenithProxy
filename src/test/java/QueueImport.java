import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenith.database.ConnectionPool;
import com.zenith.database.dto.tables.Queuelength;
import com.zenith.database.dto.tables.records.QueuelengthRecord;
import com.zenith.util.Wait;
import lombok.Builder;
import lombok.Data;
import org.jooq.DSLContext;
import org.jooq.InsertSetMoreStep;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.Test;
import reactor.netty.http.client.HttpClient;

import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

public class QueueImport {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private final HttpClient httpClient = HttpClient.create();

    @Test
    public void importQueue() {
        final String regularQUrl = "https://2b2t.io/api/queue?range=24h";
        final String prioQUrl = "https://2b2t.io/api/prioqueue?range=24h";

        List<String> regularQUrls = grabArchiveIds(regularQUrl);
        List<ParsedQueueData> regularQData = regularQUrls.stream()
                .flatMap(id -> getQueueDataAtIdAndUrl(id, regularQUrl).stream())
                .sorted(Comparator.comparing(ParsedQueueData::getTime))
                .collect(Collectors.toList());

        List<String> prioQUrls = grabArchiveIds(prioQUrl);
        List<ParsedQueueData> prioQData = prioQUrls.stream()
                .flatMap(id -> getQueueDataAtIdAndUrl(id, prioQUrl).stream())
                .sorted(Comparator.comparing(ParsedQueueData::getTime))
                .collect(Collectors.toList());
        System.out.println("data collected");
        final List<QueueData> joinedData = getJoinedData(regularQData, prioQData);
        System.out.println("data joined");
        writeQueueData(joinedData);
    }

    private void writeQueueData(List<QueueData> joinedData) {
        final ConnectionPool connectionPool = new ConnectionPool();
        final Queuelength q = Queuelength.QUEUELENGTH;
        try (final Connection connection = connectionPool.getWriteConnection()) {
            final DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);
            List<InsertSetMoreStep<QueuelengthRecord>> queries = joinedData.stream().map(qd -> context.insertInto(q)
                            .set(q.TIME, qd.instant.atOffset(ZoneOffset.UTC))
                            .set(q.REGULAR, isNull(qd.regular) ? null : qd.regular.shortValue())
                            .set(q.PRIO, isNull(qd.prio) ? null : qd.prio.shortValue()))
                    .collect(Collectors.toList());
            System.out.println("writing data to db");
            context.batch(queries).execute();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<QueueData> getJoinedData(List<ParsedQueueData> regularQData, List<ParsedQueueData> prioQData) {
        // 2b2t.io stores prio and regular as separate datastreams
        // but in our db, we always have them together
        // so our strategy here is to combine the two streams when they are relatively close (within 5 mins)
        // if one is missing we'll just write null in the other
        final Instant firstTime = regularQData.get(0).getTime();
        final TimeIterator timeIterator = new TimeIterator(Instant.ofEpochSecond(1674270000L), firstTime);
        final List<QueueData> result = new ArrayList<>();
        while (timeIterator.hasNext()) { // this is terribly inefficient but idc its all local and im only iterating through like 400k items
            final Instant nextTime = timeIterator.next();
            final Optional<ParsedQueueData> regularEntry = getEntryAtTime(nextTime, regularQData);
            final Optional<ParsedQueueData> prioEntry = getEntryAtTime(nextTime, prioQData);

            if (!regularEntry.isPresent() && !prioEntry.isPresent()) continue;

            final QueueData.QueueDataBuilder builder = new QueueData.QueueDataBuilder();
            regularEntry.ifPresent(e -> builder.regular(e.getCount()).instant(e.getTime()));
            prioEntry.ifPresent(e -> builder.prio(e.getCount()).instant(e.getTime()));
            final QueueData data = builder.build();
            result.add(data);
        }
        return result;
    }

    private Optional<ParsedQueueData> getEntryAtTime(final Instant time, final List<ParsedQueueData> data) {
        List<ParsedQueueData> regularCollect = data.stream()
                .filter(d -> d.getTime().isBefore(time) && d.getTime().isAfter(time.minus(Duration.ofMinutes(9))))
                .limit(1L)
                .collect(Collectors.toList());
        if (regularCollect.size() >= 1) {
            return Optional.of(regularCollect.get(0));
        } else {
            return Optional.empty();
        }
    }

    private List<ParsedQueueData> getQueueDataAtIdAndUrl(final String archiveId, final String url) {
        final String rawData = httpClient.get()
                .uri("https://web.archive.org/web/" + archiveId + "/" + url)
                .responseContent()
                .aggregate()
                .asString()
                .retry(2)
                .block();
        Wait.waitALittleMs(50); // rate limit
        if (isNull(rawData)) {
            return Collections.emptyList();
        }
        final List<ParsedQueueData> parsedData = parseRawQueueData(rawData);
        return parsedData;
    }

    private List<ParsedQueueData> parseRawQueueData(final String rawData) {
        try {
            List<QueueDataModel> queueDataModels = Arrays.asList(objectMapper.readValue(rawData, QueueDataModel[].class));
            List<ParsedQueueData> outList = queueDataModels.stream()
                    .map(queueDataModel -> new ParsedQueueData.ParsedQueueDataBuilder()
                            .time(Instant.ofEpochSecond(queueDataModel.timeEpoch))
                            .count(queueDataModel.count)
                            .build())
                    .collect(Collectors.toList());
            return outList;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> grabArchiveIds(final String url) {
        final String responseBody = httpClient.get()
                .uri("http://web.archive.org/cdx/search/cdx?url=" + url)
                .responseContent()
                .aggregate()
                .asString()
                .block();
        final List<String> lines = Arrays.asList(responseBody.split("\n"));
        return lines.stream()
                .filter(line -> line.contains(" "))
                .map(line -> line.split(" ")[1])
                .collect(Collectors.toList());
    }

    private static class TimeIterator implements Iterator<Instant> {

        private static final Duration timeInterval = Duration.ofMinutes(5L);
        private final Instant endTime;
        private Instant currentTime;

        private TimeIterator(final Instant endTime, final Instant startTime) {
            this.endTime = endTime;
            this.currentTime = startTime.minus(timeInterval);
        }


        @Override
        public boolean hasNext() {
            return currentTime.isBefore(endTime);
        }

        @Override
        public Instant next() {
            final Instant nextTime = currentTime;
            currentTime = currentTime.plus(timeInterval);
            return nextTime;
        }
    }

    @Data
    @Builder
    private static class QueueData { // joined prio and regular queue data
        private final Instant instant;
        private final Integer regular;
        private final Integer prio;

    }

    @JsonPropertyOrder({"timeEpoch", "count"})
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    private static class QueueDataModel {
        public int timeEpoch;
        public int count;
    }

    @Data
    @Builder
    static class ParsedQueueData { // could be for either prio or regular
        private final Instant time;
        private final int count;
    }
}
