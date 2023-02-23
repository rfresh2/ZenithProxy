import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenith.database.ConnectionPool;
import com.zenith.database.dto.tables.Playercount;
import com.zenith.database.dto.tables.Queuelength;
import com.zenith.database.dto.tables.records.PlayercountRecord;
import com.zenith.database.dto.tables.records.QueuelengthRecord;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.jooq.DSLContext;
import org.jooq.InsertSetMoreStep;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.Test;

import java.io.File;
import java.sql.Connection;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

public class PlayerCountImport {

    @Test
    public void importPlayerCount() {
        final ConnectionPool connectionPool = new ConnectionPool();
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try (final Connection connection = connectionPool.getWriteConnection()) {
            List<ParsedPlayerCount> playerCounts = objectMapper.readValue(new File("data/lolrittercommands.json"), DiscordMain.class).messages
                    .stream()
                    .filter(discordMessage -> discordMessage.author.id.equals("521791765989031957"))
                    .filter(discordMessage -> discordMessage.embeds.size() > 0)
                    .flatMap(discordMessage -> discordMessage.embeds.stream())
                    .filter(embed -> nonNull(embed.author))
                    .filter(embed -> embed.author.name.equals("2b2t server statistics"))
                    .filter(embed -> embed.fields.size() > 0)
                    .filter(embed -> embed.fields.get(0).name.equals("Players"))
                    .filter(embed -> embed.fields.get(0).value.contains("players"))
                    .map(embed -> new ParsedPlayerCount.ParsedPlayerCountBuilder()
                            .time(OffsetDateTime.parse(embed.timestamp).toInstant())
                            .count(parseCount(embed.fields.get(0).value))
                            .build())
                    .collect(Collectors.toList());

            final DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);
            Playercount pc = Playercount.PLAYERCOUNT;
            List<InsertSetMoreStep<PlayercountRecord>> queries = playerCounts.stream().map(p -> context.insertInto(pc)
                            .set(pc.COUNT, p.count.shortValue())
                            .set(pc.TIME, p.time.atOffset(ZoneOffset.UTC)))
                    .collect(Collectors.toList());
            context.batch(queries).execute();

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void importQueues() {
        final ConnectionPool connectionPool = new ConnectionPool();
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try (final Connection connection = connectionPool.getWriteConnection()) {
            List<ParsedQueueStatus> queueStatuses = objectMapper.readValue(new File("data/lolrittercommands.json"), DiscordMain.class).messages
                    .stream()
                    .filter(discordMessage -> discordMessage.author.id.equals("521791765989031957"))
                    .filter(discordMessage -> discordMessage.embeds.size() > 0)
                    .flatMap(discordMessage -> discordMessage.embeds.stream())
                    .filter(embed -> nonNull(embed.author))
                    .filter(embed -> embed.author.name.equals("2b2t server statistics"))
                    .filter(embed -> embed.fields.size() > 0)
                    .filter(embed -> embed.fields.get(1).name.equals("Normal queue"))
                    .filter(embed -> embed.fields.get(2).name.equals("Prio queue"))
                    .filter(embed -> embed.fields.get(1).value.contains("players"))
                    .filter(embed -> embed.fields.get(2).value.contains("players"))
                    .map(embed -> new ParsedQueueStatus.ParsedQueueStatusBuilder()
                            .time(OffsetDateTime.parse(embed.timestamp).toInstant())
                            .regular(parseCount(embed.fields.get(1).value))
                            .prio(parseCount(embed.fields.get(2).value))
                            .build())
                    .collect(Collectors.toList());

            final DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);
            Queuelength q = Queuelength.QUEUELENGTH;
            List<InsertSetMoreStep<QueuelengthRecord>> queries = queueStatuses.stream().map(p -> context.insertInto(q)
                            .set(q.TIME, p.time.atOffset(ZoneOffset.UTC))
                            .set(q.REGULAR, p.regular.shortValue())
                            .set(q.PRIO, p.prio.shortValue()))
                    .collect(Collectors.toList());
            context.batch(queries).execute();

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }


    private Integer parseCount(String value) {
        return Integer.parseInt(value.split(" ")[0]);
    }

    static class DiscordMain {
        @JsonProperty("messages")
        public List<DiscordMessage> messages;

    }

    @Data
    @Builder
    static class ParsedPlayerCount {
        private final Integer count;
        private final Instant time;
    }

    @Data
    @Builder
    static class ParsedQueueStatus {
        private final Integer regular;
        private final Integer prio;
        private final Instant time;
    }

    static class DiscordMessage {
        @JsonProperty("timestamp")
        public String timestamp;
        @JsonProperty("author")
        public DiscordMessageAuthor author;
        @JsonProperty("embeds")
        public List<DiscordMessageEmbed> embeds;
    }

    @ToString
    static class DiscordMessageEmbed {
        @JsonProperty("timestamp")
        public String timestamp;
        @JsonProperty("author")
        public DiscordMessageEmbedAuthor author;
        @JsonProperty("fields")
        public List<DiscordMessageEmbedField> fields;
    }

    static class DiscordMessageEmbedField {
        @JsonProperty("name")
        public String name;
        @JsonProperty("value")
        public String value;
    }

    static class DiscordMessageEmbedAuthor {
        @JsonProperty("name")
        public String name;
    }

    static class DiscordMessageAuthor {
        @JsonProperty("id")
        public String id;

    }

    static class DiscordEmbedThumbnail {
        @JsonProperty("url")
        public String url;
    }
}
